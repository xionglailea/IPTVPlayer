package cn.edu.bupt.iptvplayer.net;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Created by Administrator on 2015/3/30.
 */
public class HttpParser {
    private static final String TAG = HttpParser.class.getName();
    private String path;
    private Map<String, String> iptvLists;

    public HttpParser(String path) {
        this.path = path;
        iptvLists = new LinkedHashMap<>();
    }

    public Map<String, String> parseByrTv() {
        Document doc;
        Elements titles;
        Elements hrefs;
        try {
            doc = Jsoup.connect(path).get();
            //Elements
            Document content = Jsoup.parse(doc.toString());
            titles = content.getElementsByTag("p");
            hrefs = doc.select("a[href]");
            for (int i = 0; i < hrefs.size(); i++) {
                String linkHref = hrefs.get(i).attr("href");
                if (linkHref.endsWith(".m3u8")) {
                    iptvLists.put(titles.get(i / 2).text(), linkHref);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return iptvLists;
    }
}
