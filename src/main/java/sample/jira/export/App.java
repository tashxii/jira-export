package sample.jira.export;

import com.atlassian.jira.rest.client.api.domain.Issue;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.*;
import java.util.List;
import java.util.Properties;

/**
 * Main
 * <p>
 * Search JIRA issues and export its summaries to the specified excel file.
 * The following properties file "jiraexport.properties" is required to be located on the execution directory.
 * <pre>
 *     jira.url=url for JIRA project. e.g) https://your-project-name.atlassian.net
 *     jira.user=login user for JIRA.
 *     jira.password=password of jira.user.
 *     jira.query=jql(JIRA Query Language) string for issues. e.g) status!=done. For details, see https://confluence.atlassian.com/jirasoftwarecloud/advanced-searching-764478330.html.
 *     #proxy.host=proxy host(optional)
 *     #proxy.port=proxy port number(optional)
 * </pre>
 */
public class App {
    private static final String JIRAEXPORT_PROPERTIES = "jiraexport.properties";

    public static void main(String[] args) {
        if (args.length != 1) {
            System.out.println("Please specify output excel file path.");
            System.exit(2);
            return;
        }

        try {
            Properties props = new Properties();
            props.load(new FileInputStream(JIRAEXPORT_PROPERTIES));

            //Proxy server settings.
            configureProxy(props);

            //JiraRestClient settings.
            String baseUrl = props.getProperty("jira.url");
            String user = props.getProperty("jira.user");
            String password = props.getProperty("jira.password");
            //Query for issue.
            String jql = props.getProperty("jira.query");
            if (baseUrl == null)
                throw new RuntimeException(String.format("'%s' must be set in the %s file.", "jira.url", JIRAEXPORT_PROPERTIES));
            if (user == null)
                throw new RuntimeException(String.format("'%s' must be set in the %s file.", "jira.user", JIRAEXPORT_PROPERTIES));
            if (password == null)
                throw new RuntimeException(String.format("'%s' must be set in the %s file.", "jira.password", JIRAEXPORT_PROPERTIES));
            if (jql == null)
                throw new RuntimeException(String.format("'%s' must be set in the %s file.", "jira.query", JIRAEXPORT_PROPERTIES));

            //Search issues.
            JiraClient client = new JiraClient(baseUrl, user, password);
            List<Issue> issues = client.searchIssues(jql);
            System.out.println(String.format("%d issues are found.", issues.size()));

            //Export excel file.
            exportExcel(args[0], issues);
            System.out.println(String.format("%s file is exported.", args[0]));
            System.exit(0);
        } catch (Exception e) {
            System.out.println("An error occurred.");
            String message = (e.getMessage() != null) ? e.getMessage() : "No error message.";
            System.out.println("  Message: " + message);
            System.out.println("-- Details --");
            e.printStackTrace();
            System.exit(1);
        }
    }

    private static void configureProxy(Properties props) {
        String proxyHost = props.getProperty("proxy.host");
        if (proxyHost != null) {
            String proxyPort = props.getProperty("proxy.port");
            System.setProperty("http.proxyHost", proxyHost);
            System.setProperty("https.proxyHost", proxyHost);
            proxyPort = (proxyPort != null) ? proxyPort : "80";
            if (proxyPort != null) {
                System.setProperty("http.proxyPort", proxyPort);
                System.setProperty("https.proxyPort", proxyPort);
            }
        }
    }

    private static void exportExcel(String outputExcelPath, List<Issue> issues) throws IOException {
        Workbook book = new XSSFWorkbook();
        Sheet sheet = book.createSheet("Sheet1");
        //Font
        Font font = book.createFont();
        font.setFontName("MS UI Gothic");
        //Style
        CellStyle top = book.createCellStyle();
        top.setVerticalAlignment(VerticalAlignment.TOP);
        top.setFont(font);
        CellStyle wrap = book.createCellStyle();
        wrap.setWrapText(true);
        wrap.setFont(font);
        wrap.setVerticalAlignment(VerticalAlignment.TOP);

        //Export issues.
        createHeaderRow(sheet, top, "JIRA-ID", "Summary", "Description");
        int i = 1;
        for (Issue issue : issues) {
            Row row = sheet.createRow(i++);
            exportIssueRow(top, wrap, issue, row);
        }

        //Auto resize for column width.
        sheet.autoSizeColumn(0);
        sheet.autoSizeColumn(1);
        sheet.autoSizeColumn(2);

        saveExcelFile(outputExcelPath, book);
    }

    private static void createHeaderRow(Sheet sheet, CellStyle style, String... headers) {
        Row headerRow = sheet.createRow(0);
        int i = 0;
        for (String header : headers) {
            Cell cell = headerRow.createCell(i++);
            cell.setCellStyle(style);
            cell.setCellValue(header);
        }
    }

    private static void exportIssueRow(CellStyle top, CellStyle wrap, Issue issue, Row row) {
        for (int j = 0; j < 3; j++) {
            Cell cell = row.createCell(j);
            switch (j) {
                case 0:
                    cell.setCellStyle(top);
                    cell.setCellValue(issue.getKey());
                    break;
                case 1:
                    cell.setCellStyle(top);
                    cell.setCellValue(issue.getSummary());
                    break;
                case 2:
                    cell.setCellStyle(wrap);
                    String text = issue.getDescription();
                    if (text != null) {
                        text = text.replace("\r\n", "\n");
                        cell.setCellValue(text);
                    }
                    break;
            }
        }
    }

    private static void saveExcelFile(String outputExcelPath, Workbook book) throws IOException {
        File file = new File(outputExcelPath);
        if (file.exists()) {
            file.delete();
        }
        FileOutputStream fs = null;
        try {
            fs = new FileOutputStream(file);
            book.write(fs);
        } catch (IOException e) {
            throw e;
        } finally {
            if (fs != null) {
                try {
                    fs.close();
                } catch (IOException e) {
                    //ignore
                }
            }
        }
    }

}
