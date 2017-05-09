package sample.jira.export;

import com.atlassian.jira.rest.client.api.JiraRestClient;
import com.atlassian.jira.rest.client.api.JiraRestClientFactory;
import com.atlassian.jira.rest.client.api.SearchRestClient;
import com.atlassian.jira.rest.client.api.domain.Issue;
import com.atlassian.jira.rest.client.api.domain.SearchResult;
import com.atlassian.jira.rest.client.internal.async.AsynchronousJiraRestClientFactory;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * JIRA client
 */
public class JiraClient {
    private JiraRestClient jiraRestClient;

    public JiraClient(final String baseUrl, final String userName, final String password) throws Exception {
        JiraRestClientFactory factory = new AsynchronousJiraRestClientFactory();
        URI uri = new URI(baseUrl);
        jiraRestClient = factory.createWithBasicHttpAuthentication(uri, userName, password);
    }

    public List<Issue> searchIssues(String jql) {
        SearchRestClient searchRestClient = jiraRestClient.getSearchClient();
        final Set<String> fields = new HashSet<String>();
        SearchResult result = searchRestClient.searchJql(jql, 100, 0, fields).claim();
        Iterable<Issue> issues = result.getIssues();
        List<Issue> list = new ArrayList<Issue>();
        for (final Issue issue : issues) {
            list.add(issue);
        }
        return list;
    }
}

