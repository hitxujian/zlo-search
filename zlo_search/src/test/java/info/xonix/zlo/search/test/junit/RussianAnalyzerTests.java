package info.xonix.zlo.search.test.junit;

import info.xonix.zlo.search.LuceneVersion;
import info.xonix.zlo.search.analyzers.RussianAnalyzerImproved;
import info.xonix.zlo.search.config.Config;
import info.xonix.zlo.search.logic.MessageFields;
import info.xonix.zlo.search.spring.AppSpringContext;
import info.xonix.zlo.search.utils.IOUtil;
import info.xonix.zlo.search.utils.LuceneUtils;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.ru.RussianAnalyzer;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.Query;
import org.apache.lucene.util.Version;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * User: Vovan
 * Date: 19.03.11
 * Time: 22:02
 */
public class RussianAnalyzerTests {
    private static Analyzer analyzer;

    @BeforeClass
    public static void setUp() {
        Config config = AppSpringContext.getApplicationContextForTesting().getBean(Config.class);
        analyzer = config.getMessageAnalyzer();
        System.out.println("Using analyzer: " + analyzer.getClass());
    }

    @Test
    public void test1() throws IOException {
        checkCorrectAnalyzing(analyzer, "привет мир, !!! ййй qqq 123 123qw 123привет",
                new String[]{"привет", "мир", "ййй", "qqq", "123", "123qw", "123привет"});
    }

    @Test
    public void test2() throws IOException {
        checkCorrectAnalyzing(analyzer, "бежать 777777 хотел Кровать? и как всЕгда ,,,во cisco",
                new String[]{"бежа", "777777", "хотел", "крова", "всегд", "cisco"});
    }

    @Test
    public void test3() throws IOException {
        checkCorrectAnalyzing(analyzer, "в чем смысл жизни? Жизнь это 01234567890 Ёж ёлка",
                new String[]{"смысл", "жизн", "жизн", "01234567890", "еж", "елк"});
    }

    @Test
    public void testElka() throws IOException {
        checkCorrectAnalyzing(analyzer, "елка ёлка Ёлки ЁлКоЙ",
                new String[]{"елк", "елк", "елк", "елк"});
    }

    @Test
    public void testNostem() throws IOException {
        checkCorrectAnalyzing(analyzer, ".Net c++ C#",
                new String[]{"net", "c", "c"});
    }

    @Test
    public void test4() throws IOException, ParseException {
        String str = "a *:* bbbb \"AAA BB CCCC 111\" бы хотя";
//        String str = "*:*";
//        String str = "не не";
        System.out.println(LuceneUtils.tokenize(analyzer,MessageFields.BODY, str));

        QueryParser parser = new QueryParser(LuceneVersion.VERSION, MessageFields.BODY, analyzer);
        Query query = parser.parse(str);
        Set<Term> set = new HashSet<Term>();
        query.extractTerms(set);
        System.out.println(query);
        System.out.println(set);
    }

    @Test
    public void testDots() throws IOException {
        final String str = "aaa.bbb.com:8888 " +
                "a,b;c/d'e$f&g*h+i-j%k/l_m#n@o!p?q>r\"s~t(u`v|z}y\\z";

        checkCorrectAnalyzing(analyzer, str,
                new String[]{"aaa", "bbb", "com", "8888", "a", "b", "c", "d", "e",
                        "f", "g", "h", "i", "j", "k", "l", "m", "n", "o", "p", "q",
                        "r", "s", "t", "u", "v", "z", "y", "z"});
    }

    @Test
    public void testDotsShow() throws IOException {
        final String str = "aaa.bbb.com:8888 " +
                "a,b;c/d'e$f&g*h+i-j%k/l_m#n@o!p?q>r\"s~t(u`v|z}y\\z";

        System.out.println("New analyzer:");
        System.out.println(LuceneUtils.tokenize(new RussianAnalyzer(Version.LUCENE_36), MessageFields.BODY, str));

        System.out.println("Old analyzer:");
        System.out.println(LuceneUtils.tokenize(new RussianAnalyzer(Version.LUCENE_30), MessageFields.BODY, str));
    }

    private void checkCorrectAnalyzing(final Analyzer theAnalyzer, String str, String[] expectedResult) throws IOException {
        System.out.println("-----");

        List<String> tokens = LuceneUtils.tokenize(theAnalyzer, MessageFields.BODY, str);

        try {
            Assert.assertArrayEquals(expectedResult, tokens.toArray());
        } catch (AssertionError e) {
            System.err.println("Expected: " + Arrays.asList(expectedResult));
            System.err.println("Actual  : " + tokens);
            throw e;
        }
    }

    @Test
    public void printDefaultStopWords() {
        System.out.println(RussianAnalyzerImproved.getDefaultStopSet());
        System.out.println(IOUtil.loadStrings(RussianAnalyzerImproved.class.getResourceAsStream("zlo-russian-stop.txt")));
    }
}
