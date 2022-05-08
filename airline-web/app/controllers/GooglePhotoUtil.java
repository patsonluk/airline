package controllers;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.client.util.store.MemoryDataStoreFactory;
import com.google.api.gax.core.FixedCredentialsProvider;
import com.google.api.gax.rpc.ApiException;
import com.google.auth.oauth2.UserCredentials;
import com.google.common.collect.ImmutableList;
import com.google.photos.library.v1.PhotosLibraryClient;
import com.google.photos.library.v1.PhotosLibrarySettings;
import com.google.photos.library.v1.internal.InternalPhotosLibraryClient;
import com.google.photos.library.v1.proto.ListAlbumsRequest;
import com.google.photos.types.proto.Album;
import com.google.photos.types.proto.MediaItem;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class GooglePhotoUtil {
    private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();

    /**
     * Global instance of the scopes required by this quickstart.
     * If modifying these scopes, delete your previously saved tokens/ folder.
     */
    private static final String CREDENTIALS_FILE_PATH = "/google-oauth-credentials.json";
    private static final String TOKENS_DIRECTORY_PATH = "google-tokens";

    private static final List<String> REQUIRED_SCOPES =
            ImmutableList.of(
                    "https://www.googleapis.com/auth/photoslibrary.readonly");
    private static final String ALBUM_TITLE = "banners";
    private static final List<String> bannerUrls = new ArrayList<>();

    static {
        refreshBanners();
    }

    /**
     * Creates an authorized Credential object.
     * @param HTTP_TRANSPORT The network HTTP Transport.
     * @return An authorized Credential object.
     * @throws IOException If the credentials.json file cannot be found.
     */
    private static UserCredentials getCredentials(final NetHttpTransport HTTP_TRANSPORT) throws IOException {
        // Load client secrets.
        InputStream in = GooglePhotoUtil.class.getResourceAsStream(CREDENTIALS_FILE_PATH);
        GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));

        // Build flow and trigger user authorization request.
        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                HTTP_TRANSPORT, JSON_FACTORY, clientSecrets, REQUIRED_SCOPES)
                .setDataStoreFactory(new FileDataStoreFactory(new java.io.File(TOKENS_DIRECTORY_PATH)))
                //.setDataStoreFactory(new MemoryDataStoreFactory())
                .setAccessType("offline")
                .build();
        Credential credential = new AuthorizationCodeInstalledApp(flow, new LocalServerReceiver()).authorize("user");
        return UserCredentials.newBuilder()
                .setClientId(clientSecrets.getDetails().getClientId())
                .setClientSecret(clientSecrets.getDetails().getClientSecret())
                .setRefreshToken(credential.getRefreshToken())
                .build();
    }

    public static void refreshBanners() {
        synchronized (bannerUrls) {
            bannerUrls.clear();
            try {
                bannerUrls.addAll(loadBannerUrls());
            } catch (GeneralSecurityException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private static Random random = new Random();
    public static String drawBannerUrl() {
        synchronized (bannerUrls) {
            if (bannerUrls.isEmpty()) {
                return null;
            } else {
                return bannerUrls.get(random.nextInt(bannerUrls.size()));
            }
        }
    }


    private static List<String> loadBannerUrls() throws GeneralSecurityException, IOException {
        final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
      // Set up the Photos Library Client that interacts with the API
        PhotosLibrarySettings settings =
                PhotosLibrarySettings.newBuilder()
                        .setCredentialsProvider(
                                FixedCredentialsProvider.create(getCredentials(HTTP_TRANSPORT)))
                        .build();


        List<String> bannerUrls = new ArrayList<>();
        try (PhotosLibraryClient photosLibraryClient =
                     PhotosLibraryClient.initialize(settings)) {

// Make a request to list all albums in the user's library
            // Iterate over all the albums in this list
            // Pagination is handled automatically

            InternalPhotosLibraryClient.ListAlbumsPagedResponse listAlbumsPagedResponse = photosLibraryClient.listAlbums();
            String albumId = null;
            for (Album album : listAlbumsPagedResponse.iterateAll()) {
                if (ALBUM_TITLE.equals(album.getTitle())) {
                    albumId = album.getId();
                    break;
                }
            }
            if (albumId == null) {
                System.err.println("no album found with title " + ALBUM_TITLE);
            }


            // Make a request to list all media items in an album
            // Provide the ID of the album as a parameter in the searchMediaItems call
            // Iterate over all the retrieved media items
            InternalPhotosLibraryClient.SearchMediaItemsPagedResponse response = photosLibraryClient.searchMediaItems(albumId);

            for (MediaItem item : response.iterateAll()) {
                bannerUrls.add(item.getBaseUrl());
            }
        } catch (ApiException e) {
            e.printStackTrace();
        }
        return bannerUrls;
    }

    public static void main(String[] args) throws GeneralSecurityException, IOException {
        loadBannerUrls();
    }
}
