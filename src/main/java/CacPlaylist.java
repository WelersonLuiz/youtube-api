import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class CacPlaylist {

    private static final String CLIENT_SECRETS= "client_secret.json";
    private static final Collection<String> SCOPES = Collections.singletonList("https://www.googleapis.com/auth/youtube");
    private static final String APPLICATION_NAME = "API code samples";
    private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();
    private static YouTube youtubeService;

    public static Credential authorize(final NetHttpTransport httpTransport) throws IOException {
        // Load client secrets.
        InputStream in = CacPlaylist.class.getResourceAsStream(CLIENT_SECRETS);
        GoogleClientSecrets clientSecrets = GoogleClientSecrets
                .load(JSON_FACTORY, new InputStreamReader(in));

        // Build flow and trigger user authorization request.
        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow
                .Builder(httpTransport, JSON_FACTORY, clientSecrets, SCOPES)
                .build();

        return new AuthorizationCodeInstalledApp(flow, new LocalServerReceiver())
                .authorize("user");
    }

    public static YouTube getService() throws GeneralSecurityException, IOException {
        final NetHttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();
        Credential credential = authorize(httpTransport);
        return new YouTube.Builder(httpTransport, JSON_FACTORY, credential)
                .setApplicationName(APPLICATION_NAME)
                .build();
    }

    public static void main(String[] args) throws GeneralSecurityException, IOException, InterruptedException {
        youtubeService = getService();

        String cacId = "UCtrjFP7i92_30uv6IehwE5Q";
        String myPlaylist = "PL6VkRz5e89jT8aqVIYSq3aFd1NBWMtPT8";
        String cacUploads = getUploadPlaylistId(cacId);
        List<String> cacVideos = getPlaylistItems(cacUploads);

        // Sublista gerada pela limitação de cota diária
        int insertedVideos = 784;
        cacVideos = cacVideos.subList(insertedVideos, cacVideos.size());
        
        insertIntoPlaylist(cacVideos, myPlaylist);
    }

    public static String getUploadPlaylistId(String channelId) throws IOException {
        YouTube.Channels.List request = youtubeService
                .channels()
                .list("snippet,contentDetails,statistics");

        ChannelListResponse response = request.setId(channelId).execute();
        String uploadPlaylistId = response.getItems().get(0).getContentDetails().getRelatedPlaylists().getUploads();

        System.out.println("Upload Playlist Id: " + uploadPlaylistId);
        return uploadPlaylistId;
    }

    public static List<String> getPlaylistItems(String playlistId) throws IOException {
        YouTube.PlaylistItems.List request = youtubeService
                .playlistItems()
                .list("snippet")
                .setMaxResults(50L);

        List<String> videoIdList = new ArrayList<>();
        PlaylistItemListResponse response = new PlaylistItemListResponse();

        do {
            response = request.setPageToken(response.getNextPageToken()).setPlaylistId(playlistId).execute();
            for (PlaylistItem item : response.getItems()) {
                videoIdList.add(item.getSnippet().getResourceId().getVideoId());
            }
        } while (response.getNextPageToken() != null && !response.getNextPageToken().equals(""));

        System.out.println("Video Ids: " + videoIdList);
        return videoIdList;
    }

    public static void insertIntoPlaylist(List<String> videoIdList, String playlistId) throws IOException, InterruptedException {


        for (String video : videoIdList) {
            System.out.println("Inserting video: " + video);
            PlaylistItem playlistItem = new PlaylistItem();

            PlaylistItemSnippet snippet = new PlaylistItemSnippet();
            snippet.setPlaylistId(playlistId);
            snippet.setPosition(0L);
            ResourceId resourceId = new ResourceId();
            resourceId.setKind("youtube#video");
            resourceId.setVideoId(video);
            snippet.setResourceId(resourceId);
            playlistItem.setSnippet(snippet);

            YouTube.PlaylistItems.Insert request = youtubeService.playlistItems()
                    .insert("snippet", playlistItem);
            request.execute();

            Thread.sleep(1500);
        }
    }

}