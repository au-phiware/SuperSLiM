/*
 */
package com.tonicartos.superslim.restexample;

import static java.lang.String.format;
import static java.util.Arrays.copyOf;
import static android.os.AsyncTask.Status.RUNNING;

import android.os.AsyncTask;
import android.support.v4.util.LruCache;
import android.support.v4.util.Pair;
import android.widget.TextView;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GitHubClient {
    GitHubAdapter adapter;

    public GitHubClient(GitHubAdapter adapter) {
        this.adapter = adapter;
    }

    public interface Batch {
        public URL getURL();
    }

    public class UserBatch implements Batch {
        public final static int COUNT = 30;
        public final URL url;
        public final JSONArray users;
        public final long batchNumber;
        public final long nextBatchNumber;

        UserBatch(URL url, JSONArray json, long batchNumber, long nextBatchNumber) {
            this.url = url;
            this.users = json;
            this.batchNumber = batchNumber;
            this.nextBatchNumber = nextBatchNumber;
        }

        public URL getURL() {
            return url;
        }

        public JSONObject getUser(int index) throws JSONException {
            return users.getJSONObject(index);
        }
    }

    public class RepoBatch implements Batch {
        public final static int COUNT = 30;
        public final URL url;
        public final JSONArray repos;
        public final long userId;
        public final int pageNumber;
        public final int lastPageNumber;

        RepoBatch(URL url, JSONArray json, long userId, int pageNumber, int lastPageNumber) {
            this.url = url;
            this.repos = json;
            this.userId = userId;
            this.pageNumber = pageNumber;
            this.lastPageNumber = lastPageNumber;
        }

        public URL getURL() {
            return url;
        }

        public JSONObject getRepo(int index) throws JSONException {
            return repos.getJSONObject(index);
        }

        public boolean isLast() {
            return lastPageNumber < 2 || pageNumber == lastPageNumber;
        }
    }

    private LruCache<Long, UserBatch> userBatches = new LruCache<Long, UserBatch>(30) {
        private Request<UserBatch> task = new Request<UserBatch>() {
            Pattern sincePattern = Pattern.compile("since=(\\d+)");
            protected UserBatch createBatch(URL url, String etag, String json, Map<String, String> links) throws JSONException {
                long batchNumber = 0;
                long nextBatchNumber = 0;
                String query = url.getQuery();
                if (query != null) {
                    Matcher match = sincePattern.matcher(query);
                    if (match.find()) {
                        batchNumber = Long.parseLong(match.group(1));
                    }
                }
                String next = links.get("next");
                if (next != null) {
                    Matcher match = sincePattern.matcher(next);
                    if (match.find()) {
                        nextBatchNumber = Long.parseLong(match.group(1));
                    }
                }
                return new UserBatch(url, new JSONArray(json), batchNumber, nextBatchNumber);
            }

            protected void onProgressUpdate(UserBatch... batches) {
                for (UserBatch batch : batches) {
                    put(batch.batchNumber, batch);
                    adapter.notifyUserChanged(batch);
                }
            }
        };

        protected UserBatch create(Long since) {
            try {
                task.add(new URL(format("https://api.github.com/users?since=%d", since)));
            } catch(MalformedURLException ignored) {}
            return null;
        }
    };

    public UserBatch getUserBatch(Long batchNumber) {
        return userBatches.get(batchNumber);
    }

    private LruCache<Pair<Long, Integer>, RepoBatch> repoBatches = new LruCache<Pair<Long, Integer>, RepoBatch>(30) {
        private Request<RepoBatch> task = new Request<RepoBatch>() {
            Pattern pagePattern = Pattern.compile("page=(\\d+)");
            protected RepoBatch createBatch(URL url, String etag, String json, Map<String, String> links) throws JSONException {
                long userId;
                int pageNumber = 1;
                int lastPageNumber = 1;

                String[] pathComponents = url.getPath().split("/");
                userId = Long.parseLong(pathComponents[2]);

                String query = url.getQuery();
                if (query != null) {
                    Matcher match = pagePattern.matcher(query);
                    if (match.find()) {
                        pageNumber = Integer.parseInt(match.group(1));
                    }
                }

                String last = links.get("last");
                if (last != null) {
                    Matcher match = pagePattern.matcher(last);
                    if (match.find()) {
                        lastPageNumber = Integer.parseInt(match.group(1));
                    }
                }
                return new RepoBatch(url, new JSONArray(json), userId, pageNumber, lastPageNumber);
            }

            protected void onProgressUpdate(RepoBatch... batches) {
                super.onProgressUpdate(batches);
                for (RepoBatch batch : batches) {
                    put(Pair.create(batch.userId, batch.pageNumber), batch);
                    adapter.notifyRepoChanged(batch);
                }
            }
        };

        protected RepoBatch create(Pair<Long, Integer> pair) {
            Long userId = pair.first;
            Integer pageNumber = pair.second;

            try {
                task.add(new URL(format("https://api.github.com/users/%d/repos?page=%d", userId, pageNumber)));
            } catch(MalformedURLException ignored) {}
            return null;
        }
    };

    public RepoBatch getRepoBatch(long userId, int pageNumber) {
        return repoBatches.get(Pair.create(userId, pageNumber));
    }

    private static abstract class Request<B extends Batch> extends AsyncTask<URL, B, Exception[]> {
        protected abstract B createBatch(URL url, String etag, String json, Map<String, String> links) throws JSONException;

        private static Pattern linkPattern = Pattern.compile("\\s*<([^>]+)>;\\s*rel=\"([^\"]+)\",?");
        protected Exception[] doInBackground(URL... endpoints) {
            char[] buffer = new char[512]; int n;
            int count = endpoints.length;
            int err = 0;
            Exception[] errors = new Exception[count];

            for (URL url : endpoints) {
                String etag = null;
                Map<String, String> links = new HashMap<String, String>();
                String link;
                HttpURLConnection connection = null;
                try {
                    connection = (HttpURLConnection) url.openConnection();

                    // TODO: Worry about charset and content type...
                    Reader response = new InputStreamReader(connection.getInputStream());
                    StringWriter json = new StringWriter(connection.getContentLength());
                    while ((n = response.read(buffer)) != -1) json.write(buffer, 0, n);

                    etag = connection.getHeaderField("ETag");
                    link = connection.getHeaderField("Link");

                    Matcher match = linkPattern.matcher(link);
                    while (match.find()) {
                        links.put(match.group(2), match.group(1));
                    }

                    publishProgress(createBatch(url, etag, json.toString(), links));
                } catch(Exception e) {
                    // TODO: connection.getErrorStream()
                    errors[err++] = e;
                } finally {
                    if (connection != null) connection.disconnect();
                }
                if (isCancelled()) break;
            }

            return copyOf(errors, err);
        }

        private Set<URL> inProgress = new HashSet<URL>();
        private Set<URL> waiting = new HashSet<URL>();

        public boolean add(URL... endpoints) {
            boolean any = false;
            for (URL url : endpoints) {
                if (!inProgress.contains(url) && !waiting.contains(url)) {
                    waiting.add(url);
                    any = true;
                }
            }
            if (getStatus() != RUNNING && !waiting.isEmpty()) {
                inProgress = waiting;
                waiting = new HashSet();
                execute(inProgress.toArray(new URL[inProgress.size()]));
            }
            return any;
        }

        protected void onProgressUpdate(B... batches) {
            for (Batch b : batches)
                inProgress.remove(b.getURL());
        }

        protected void onPostExecute(Exception[] errors) {
            if (!waiting.isEmpty()) {
                inProgress = waiting;
                waiting = new HashSet();
                execute(inProgress.toArray(new URL[inProgress.size()]));
            }
        }
    }
}
