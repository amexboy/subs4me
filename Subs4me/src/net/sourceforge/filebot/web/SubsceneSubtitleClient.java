
package net.sourceforge.filebot.web;


import static net.sourceforge.filebot.web.WebRequest.getHtmlDocument;
import static net.sourceforge.tuned.XPathUtilities.getAttribute;
import static net.sourceforge.tuned.XPathUtilities.getChild;
import static net.sourceforge.tuned.XPathUtilities.getChildren;
import static net.sourceforge.tuned.XPathUtilities.getTextContent;
import static net.sourceforge.tuned.XPathUtilities.selectNodes;
import static net.sourceforge.tuned.XPathUtilities.selectString;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.Icon;

import net.sourceforge.filebot.ResourceManager;
import net.sourceforge.filebot.Settings;
import net.sourceforge.tuned.FileUtilities;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;


public class SubsceneSubtitleClient implements SubtitleProvider {
	
	private static final String host = "subscene.com";
	
	private final Map<String, String> languageFilterMap = initLanguageFilterMap();
	

	@Override
	public String getName() {
		return "Subscene";
	}
	

	@Override
	public URI getLink() {
		return URI.create("http://subscene.com");
	}
	

	@Override
	public Icon getIcon() {
		return ResourceManager.getIcon("search.subscene");
	}
	

	@Override
	public List<SearchResult> search(String query) throws IOException, SAXException {
		
		URL searchUrl = new URL("http", host, "/filmsearch.aspx?q=" + URLEncoder.encode(query, "UTF-8"));
		
		Document dom = getHtmlDocument(searchUrl);
		
		List<Node> nodes = selectNodes("id('filmSearch')/A", dom);
		
		List<SearchResult> searchResults = new ArrayList<SearchResult>(nodes.size());
		
		for (Node node : nodes) {
			String title = getTextContent(node);
			String href = getAttribute("href", node);
			
			try {
				searchResults.add(new HyperLink(title, new URL("http", host, href)));
			} catch (MalformedURLException e) {
				Logger.getLogger(getClass().getName()).log(Level.WARNING, "Invalid href: " + href, e);
			}
		}
		
		// we might have been redirected to the subtitle list
		if (searchResults.isEmpty()) {
			try {
				// get name of current search result
				String name = selectString("id('leftWrapperWide')//H1/text()", dom);
				
				// get current location
				String file = selectString("id('aspnetForm')/@action", dom);
				
				if (!name.isEmpty() && !file.isEmpty()) {
					searchResults.add(new HyperLink(name, new URL("http", host, file)));
				}
			} catch (Exception e) {
				Logger.getLogger(getClass().getName()).log(Level.WARNING, "Cannot parse subtitle page: " + searchUrl, e);
			}
		}
		
		return searchResults;
	}
	

	@Override
	public List<SubtitleDescriptor> getSubtitleList(SearchResult searchResult, String languageName) throws Exception {
		URL subtitleListUrl = getSubtitleListLink(searchResult, languageName).toURL();
		
		String languageFilter = null;
		
		if (languageName != null) {
			synchronized (languageFilterMap) {
				languageFilter = languageFilterMap.get(languageName.toLowerCase());
			}
		}
		
		Document subtitleListDocument = getSubtitleListDocument(subtitleListUrl, languageFilter);
		
		// let's update language filters if they are not known yet
		if (languageName != null && languageFilter == null) {
			synchronized (languageFilterMap) {
				languageFilterMap.putAll(getLanguageFilterMap(subtitleListDocument));
			}
		}
		
		return getSubtitleList(subtitleListUrl, languageName, subtitleListDocument);
	}
	

	private List<SubtitleDescriptor> getSubtitleList(URL subtitleListUrl, String languageName, Document subtitleListDocument) {
		
		List<Node> nodes = selectNodes("//TABLE[@class='filmSubtitleList']//A[@class='a1']", subtitleListDocument);
		
		// match subtitleId and typeId 
		Pattern hrefPattern = Pattern.compile("javascript:Subtitle\\((\\d+), '(\\w+)', .*");
		
		List<SubtitleDescriptor> subtitles = new ArrayList<SubtitleDescriptor>(nodes.size());
		
		for (Node node : nodes) {
			try {
				String lang = getTextContent(getChildren("SPAN", node).get(0));
				
				if (languageName == null || languageName.equalsIgnoreCase(lang)) {
					String name = getTextContent(getChildren("SPAN", node).get(1));
					String href = getAttribute("href", node);
					
					Matcher matcher = hrefPattern.matcher(href);
					
					if (!matcher.matches())
						throw new IllegalArgumentException("Cannot parse download parameters: " + href);
					
					String subtitleId = matcher.group(1);
					String archiveType = matcher.group(2);
					
					URL downloadUrl = getDownloadLink(subtitleListUrl, subtitleId, archiveType);
					
					subtitles.add(new SubsceneSubtitleDescriptor(name, lang, archiveType, downloadUrl, subtitleListUrl));
				}
			} catch (Exception e) {
				Logger.getLogger(getClass().getName()).log(Level.WARNING, "Cannot parse subtitle node", e);
			}
		}
		
		return subtitles;
	}
	

	protected Document getSubtitleListDocument(URL subtitleListUrl, String languageFilter) throws IOException, SAXException {
		URLConnection connection = subtitleListUrl.openConnection();
		
		if (languageFilter != null) {
			connection.addRequestProperty("Cookie", "subscene_sLanguageIds=" + languageFilter);
		}
		
		return getHtmlDocument(connection);
	}
	

	protected Map<String, String> initLanguageFilterMap() {
		return Settings.forPackage(SublightSubtitleClient.class).node("subtitles/subscene/languageFilterMap").asMap();
	}
	

	protected Map<String, String> getLanguageFilterMap(Document subtitleListDocument) {
		Map<String, String> filters = new HashMap<String, String>(50);
		
		List<Node> nodes = selectNodes("//DIV[@class='languageList']/DIV", subtitleListDocument);
		
		for (Node node : nodes) {
			// select INPUT/@onclick, then ditch non-number-characters
			String filter = getAttribute("onclick", getChild("INPUT", node)).replaceAll("\\D+", "");
			
			if (filter != null) {
				// select LABEL/text()
				String name = getTextContent("LABEL", node);
				
				filters.put(name.toLowerCase(), filter);
			}
		}
		
		return filters;
	}
	

	protected URL getDownloadLink(URL referer, String subtitleId, String typeId) throws MalformedURLException {
		String basePath = FileUtilities.getNameWithoutExtension(referer.getFile());
		String path = String.format("%s-dlpath-%s/%s.zipx", basePath, subtitleId, typeId);
		
		return new URL(referer.getProtocol(), referer.getHost(), path);
	}
	

	@Override
	public URI getSubtitleListLink(SearchResult searchResult, String languageName) {
		return ((HyperLink) searchResult).getURI();
	}
	
}
