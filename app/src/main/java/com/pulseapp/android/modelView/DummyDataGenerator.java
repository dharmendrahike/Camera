package com.pulseapp.android.modelView;

import android.os.Handler;
import android.util.Log;

import com.pulseapp.android.firebase.FireBaseKEYIDS;
import com.pulseapp.android.models.MomentModel;
import com.pulseapp.android.models.RoomsModel;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Random;

/**
 * Created by deepankur on 6/5/16.
 */
public class DummyDataGenerator implements FireBaseKEYIDS {
    private static DummyDataGenerator ourInstance = new DummyDataGenerator();
    private static String dummyImageUrl = "https://fbcdn-profile-a.akamaihd.net/hprofile-ak-xal1/v/" +
            "t1.0-1/p200x200/12250126_10208445021347636_2561443016702964037_n.jpg" +
            "?oh=3ff96a5cd4998cf456ba4cc46beef257&oe=57814B01&__gda__=1467850419_eec8a33ffcd3b6fee17b4257b1de05d4";
    String namesArray[] = new String[]{"January", "February", "March", "April", "May",
            "June", "July", "August", "September", "October",
            "November", "December", "Aa", "Ab"};
    private String TAG = getClass().getCanonicalName();
    private String[] dummyImagesUrl = new String[]{"http://politibits.blogs.tuscaloosanews.com/files/2010/07/sanford_big_dummy_navy_shirt.jpg",
            "https://pbs.twimg.com/profile_images/1620149654/avatar.jpg",
            "https://pbs.twimg.com/profile_images/684144749855567873/5ysjqVoC.jpg"};

    public String[]getDummyImagesUrls(){
        return dummyImagesUrl;
    }
    public static DummyDataGenerator getInstance() {
        return ourInstance;
    }

    private DummyDataGenerator() {
    }

    public ArrayList<HomeMomentViewModel> getUnseenFriendMoment() {

        ArrayList<HomeMomentViewModel> homeMomentViewModelArrayList = new ArrayList<>();
        int downloadStatus = READY_TO_VIEW_MOMENT;
        for (int i = 0; i < 100; i++) {

            if (i % 3 == 0)
                downloadStatus = READY_TO_VIEW_MOMENT;
            if (i % 3 == 1)
                downloadStatus = UNSEEN_MOMENT;
            if (i % 3 == 2)
                downloadStatus = DOWNLOADING_MOMENT;
            homeMomentViewModelArrayList.add(new
                    HomeMomentViewModel(String.valueOf("room no." + i),
                    namesArray[i % 10],
                    dummyImageUrl,
                    (i + " min "),
                    787,
                    "jhfgdg",
                    i == 3 || i == 7,
                    FRIEND_ROOM,
                    downloadStatus,null));
        }
        return homeMomentViewModelArrayList;
    }

    public ArrayList<HomeMomentViewModel> getSeenFriendMoments() {
        String namesArray[] = new String[]{"January", "February", "March", "April", "May",
                "June", "July", "August", "September", "October",
                "November", "December", "Aa", "Ab"};
        ArrayList<HomeMomentViewModel> homeMomentViewModelArrayList = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            homeMomentViewModelArrayList.add(new
                    HomeMomentViewModel(String.valueOf("room no." + i),
                    namesArray[i],
                    dummyImageUrl,
                    (i + " min "),
                    787,
                    "agsg",
                    i == 3 || i == 7,
                    FRIEND_ROOM,
                    i == 5 ? UNSEEN_MOMENT : READY_TO_VIEW_MOMENT,null));
        }
        return homeMomentViewModelArrayList;
    }

    public ArrayList<SliderMessageModel> getSliderMessageList() {
        ArrayList<SliderMessageModel> arrayList = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            SliderMessageModel model = new SliderMessageModel(namesArray[i], dummyImageUrl, String.valueOf(i), "", FRIEND_ROOM, 1,0, null);
            arrayList.add(model);
        }
        return arrayList;
    }

    public ArrayList<MomentModel.Media> getMyMediaList() {
        ArrayList<MomentModel.Media> mediaList = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            MomentModel.Media media = new MomentModel.
                    Media(dummyImageUrl, 0, 0, 0, null, i * 2, i + " min",null);
            mediaList.add(media);
        }
        return mediaList;
    }

    MyStreamListener myStreamListener;

    public void setMyStreamListener(MyStreamListener myStreamListener) {
        this.myStreamListener = myStreamListener;
        myStreamListener.onMyStreamListChanged(getMyMediaList());
    }

    public interface MyStreamListener {
        void onMyStreamListChanged(ArrayList<MomentModel.Media> medias);
    }


    LinkedHashMap<String, RoomsModel.Messages> messagesLinkedHashMap;
    ChatListener chatListener;
    Handler messageHandler;
    Runnable r = new Runnable() {
        @Override
        public void run() {
            RoomsModel.Messages messages = new RoomsModel.Messages(
                    messagesLinkedHashMap.size() % 2 == 0 ? "56f150db54108da8110fe627" : "friendId",
                    "mediaId",
                    getRandomNumbers(1, 3),
                    messagesLinkedHashMap.size() + " " + getRandomLengthString(),
                    System.currentTimeMillis(),
                    null,
                    getRandomNumbers(1, 4), null);
            messagesLinkedHashMap.put(messagesLinkedHashMap.size() + "", messages);
            chatListener.onMessageRefreshed(messagesLinkedHashMap);
            if (messagesLinkedHashMap.size() < 20)
                messageHandler.postDelayed(this, 1000);

        }
    };

    public String getRandomLengthString() {
        String[] strings = new String[]{"Hello", "whatsUp", "no budy", "maximum", "haduke"};
        String stringBuilder = "";
        if (messagesLinkedHashMap.size() % 5 == 0) {
            for (int i = 0; i < 45; i++) {
                stringBuilder += " \n";
                stringBuilder += strings[new Random().nextInt(strings.length - 1) + 1];
            }
            return stringBuilder;
        }
        return strings[0];
    }

    public void setChatListener(ChatListener chatListener) {
        this.chatListener = chatListener;
        this.chatListener.onMessageRefreshed(null);
        this.chatListener.onMessageRefreshed(messagesLinkedHashMap);
        messageHandler = new Handler();
        messagesLinkedHashMap = new LinkedHashMap<>();
        messageHandler.post(r);
    }

    public interface ChatListener {
        void onMessageRefreshed(LinkedHashMap<String, RoomsModel.Messages> messagesLinkedHashMap);
    }

    private int getRandomNumbers(int Low, int High) {
        for (int i = 0; i < 40; i++) {
            Log.d(TAG, "random " + new Random().nextInt(5 - 1) + 1);
        }
        return new Random().nextInt(High - Low) + Low;
    }

    public ArrayList<SliderMessageModel> getAllChats(int sizeOfTheList) {
        ArrayList<SliderMessageModel> models = new ArrayList<>();
        for (int i = 0; i < sizeOfTheList; i++) {
            SliderMessageModel messageModel = new SliderMessageModel();
            messageModel.displayName = generateString(7);
            messageModel.imageUrl = dummyImageUrl;
            messageModel.roomType = i % 2 == 0 ? FRIEND_ROOM : GROUP_ROOM;
            models.add(messageModel);
        }
        return models;
    }

    public static String generateString(int length) {
        String characters = "asdfghjklqwertyuiopzxcvbnm";
        Random rng = new Random();
        char[] text = new char[length];
        for (int i = 0; i < length; i++)
            text[i] = characters.charAt(rng.nextInt(characters.length()));
        String s = new String(text);
        return s.substring(0, 1).toUpperCase() + s.substring(1);
    }
}
