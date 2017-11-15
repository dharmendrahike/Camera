package com.pulseapp.android.adapters;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.media.ThumbnailUtils;
import android.os.AsyncTask;
import android.provider.MediaStore;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;


import com.pulseapp.android.R;
import com.pulseapp.android.activities.CameraActivity;
import com.pulseapp.android.firebase.FireBaseHelper;
import com.pulseapp.android.firebase.FireBaseKEYIDS;
import com.pulseapp.android.fragments.ChatFragment;
import com.pulseapp.android.modelView.ChatMediaModel;
import com.pulseapp.android.models.RoomsModel;
import com.pulseapp.android.util.AppLibrary;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

import java.io.File;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Created by deepankur on 19/4/16.
 */

public class ChatAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> implements FireBaseKEYIDS {

    private static final int VIEW_TYPE_NO_CHATS = 11111, VIEW_TYPE_NORMAL = 55555, VIEW_TYPE_SYSTEM_MESSAGE = 6666;
    private final LinkedHashMap<String, RoomsModel.Messages> unseenDownloadedChatMediaMap;
    private final int imageWidth;
    private final int imageHeight;
    private final LinearLayout.LayoutParams privateParams;
    private final LinearLayout.LayoutParams publicParams;
    private Context context;
    private FireBaseHelper mFireBaseHelper;
    private String myUserId;
    private LinkedHashMap<String, RoomsModel.Messages> messagesLinkedHashMap;
    private String chatBoxDisplayName;
    private String TAG = this.getClass().getSimpleName();
    private RecyclerViewClickInterface clickInterface;
    private ChatViewController chatViewController;
    private ViewControlCallbacks viewControlCallbacks;
    private boolean isDeletionAllowed;
    private final int dpinPixels;
    private int privateMessageHeight;
    private int publicMessageHeight;

    private RoomsModel.Messages getMessageByIndex(int index) {
        String[] keySet = messagesLinkedHashMap.keySet().toArray(new String[messagesLinkedHashMap.size()]);
        return messagesLinkedHashMap.get(keySet[index]);
    }

    public void setChatBoxDisplayName(String chatBoxDisplayName) {
        this.chatBoxDisplayName = chatBoxDisplayName;
    }

    int roomType;

    public ChatAdapter(int roomType, ChatFragment fragment, Boolean deleteUnderProgress, Context context,
                       LinkedHashMap<String, RoomsModel.Messages> messageMap,
                       RecyclerViewClickInterface clickInterface, LinkedHashMap<String, RoomsModel.Messages> unseenDownloadedChatMediaMap) {
        isDeletionAllowed = true;
        this.roomType = roomType;
        this.chatViewController = fragment;
        this.context = context;
        viewControlCallbacks = (ViewControlCallbacks) context;
        this.messagesLinkedHashMap = messageMap;
        this.clickInterface = clickInterface;
        this.deleteUnderProgress = deleteUnderProgress;
        this.unseenDownloadedChatMediaMap = unseenDownloadedChatMediaMap;
        this.mFireBaseHelper = FireBaseHelper.getInstance(context);
        myUserId = FireBaseHelper.getInstance(context).getMyUserId();
        imageWidth = (int) (AppLibrary.getDeviceParams((CameraActivity) context, "width") * 0.7);
        imageHeight = AppLibrary.convertDpToPixels(context, 80);
        this.dpinPixels = AppLibrary.convertDpToPixels(context, 1);
        privateMessageHeight = AppLibrary.convertDpToPixels(context,44);
        publicMessageHeight = AppLibrary.convertDpToPixels(context,110);
        privateParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,privateMessageHeight);
        publicParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,publicMessageHeight);
    }

    public void setDeletionAllowed(boolean deletionAllowed) {
        isDeletionAllowed = deletionAllowed;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, final int viewType) {
        if (viewType == VIEW_TYPE_NORMAL) {
            View view = LayoutInflater.from(parent.getContext()).
                    inflate(R.layout.chat_item, parent, false);
            return new MessageVH(view);
        }
        if (viewType == VIEW_TYPE_NO_CHATS)
            return new HeaderVH(LayoutInflater.from(parent.getContext()).
                    inflate(R.layout.no_chat_items_to_display, parent, false));
        if (viewType == VIEW_TYPE_SYSTEM_MESSAGE)
            return new SystemMessageVH((LayoutInflater.from(parent.getContext()).
                    inflate(R.layout.card_chat_system_message, parent, false)));

        throw new RuntimeException("Only two types of items allowed in here");
    }

    @Override
    public void onViewRecycled(RecyclerView.ViewHolder holder) {
        super.onViewRecycled(holder);
        if (holder instanceof MessageVH) {
            MessageVH messageHolder = (MessageVH) holder;
            if (messageHolder.imageTask != null) {
                if (messageHolder.imageTask.getStatus() == AsyncTask.Status.RUNNING || messageHolder.imageTask.getStatus() == AsyncTask.Status.PENDING)
                    ((AsyncTask) messageHolder.imageTask).cancel(true);
                else
                    messageHolder.imageTask.cleanup();

                messageHolder.imageTask = null;
            } else {
                Picasso.with(context).cancelRequest(messageHolder.mediaIv);
            }

            messageHolder.mediaIv.setImageResource(0);
        }
    }

    @Override
    public void onBindViewHolder(final RecyclerView.ViewHolder holder, int position) {
        if (messagesLinkedHashMap == null || messagesLinkedHashMap.size() == 0)
            return;
        final RoomsModel.Messages messages = getMessageByIndex(position);
        if (messages.type == SYSTEM_MESSAGE) {
            final SystemMessageVH systemMessageVH = (SystemMessageVH) holder;
            systemMessageVH.systemMessageTv.setText(messages.text);
            Log.d(TAG, " system message recieved");
            return;
        }


        boolean amITheAuthorOfThisMsg = messages.memberId.equals(myUserId);//for this message only


        final MessageVH messageHolder = (MessageVH) holder;

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        if (position == 0) {
            lp.setMargins(16 * dpinPixels, 16 * dpinPixels, 16 * dpinPixels, 2 * dpinPixels);
        }else {
            lp.setMargins(16 * dpinPixels, 2 * dpinPixels, 16 * dpinPixels, 2 * dpinPixels);
        }
        ((MessageVH) holder).rootView.setLayoutParams(lp);

        messageHolder.rootView.setTag(messages);
        if (position != 0 && getMessageByIndex(position - 1).type != SYSTEM_MESSAGE) {
            RoomsModel.Messages previousMessage = getMessageByIndex(position - 1);
            boolean amIAuthorOfPreviousMsg = previousMessage.memberId.equals(myUserId);
            if (roomType == GROUP_ROOM)
                messageHolder.senderNameTv.setVisibility(amIAuthorOfPreviousMsg == amITheAuthorOfThisMsg ?
                        View.GONE : View.VISIBLE);
            if (amITheAuthorOfThisMsg != amIAuthorOfPreviousMsg)
                messageHolder.senderNameTv.setText(amITheAuthorOfThisMsg ? "Me" : chatBoxDisplayName);
        } else {
            if (roomType == GROUP_ROOM) {
                messageHolder.senderNameTv.setVisibility(View.VISIBLE);
                messageHolder.senderNameTv.setText(amITheAuthorOfThisMsg ? "Me" : chatBoxDisplayName);
            }
        }

        messageHolder.rootView.setGravity(amITheAuthorOfThisMsg ? Gravity.END : Gravity.START);
        messageHolder.messageDataFrame.setBackgroundResource(amITheAuthorOfThisMsg ?
                R.drawable.sent_message__blue_bg : R.drawable.left);

        messageHolder.stringTv.setText(messages.text);
        messageHolder.timeTv.setText(AppLibrary.timeAccCurrentTime(messages.createdAt));

        //deletion code
        if (deletionListMap.get(messages.messageId) != null)
            messageHolder.rootView.setBackgroundColor(deletionListMap.get(messages.messageId) ?
                    deleteColor : normalColor);
        else messageHolder.rootView.setBackgroundColor(normalColor);

        messageHolder.mediaFrame.setVisibility(messages.type == MESSAGE_TYPE_TEXT ? View.GONE : View.VISIBLE);
        messageHolder.stringTv.setVisibility((messages.type == MESSAGE_TYPE_TEXT || (messages.type == MESSAGE_TYPE_MEDIA)) ? View.VISIBLE : View.GONE);

        if ((messages.type == MESSAGE_TYPE_MEDIA || messages.type == MESSAGE_TYPE_MEDIA_WITH_TEXT) && messages.mediaStatus != null) {
            if (messages.expiryType == VIEW_FOR_A_DAY){
                messageHolder.messageDataFrame.setLayoutParams(publicParams);
            } else {
                messageHolder.messageDataFrame.setLayoutParams(privateParams);
            }
            if (messages.expiryType != VIEW_ONCE_AND_VIEWED) {
                messageHolder.mediaIv.setVisibility(View.VISIBLE);
                if (messagesLinkedHashMap.containsKey(messages.messageId)) {
                    messages.position = position;
                    Integer status = messages.mediaStatus;
                    if (status == ERROR_DOWNLOADING_MEDIA) {
                        if (!messages.memberId.equals(mFireBaseHelper.getMyUserId())) {
                            // uploading failed
                            messageHolder.timeTv.setText(MEDIA_DOWNLOADING_FAILED_TEXT);
                            messageHolder.messageDataFrame.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    messageHolder.timeTv.setText(MEDIA_DOWNLOADING_TEXT);
                                    // retry uploading of this media
                                    mFireBaseHelper.retryDownloadingChatMedia(messages);
                                }
                            });
                            messageHolder.progressBar.setVisibility(View.GONE);
                            messageHolder.mediaIv.setVisibility(View.VISIBLE);
                            messageHolder.mediaFrame.setVisibility(View.VISIBLE);
                        }
                    } else if (status == MEDIA_DOWNLOADING) {
                        messageHolder.messageDataFrame.setOnClickListener(null);
                        messageHolder.mediaFrame.setVisibility(View.VISIBLE);
                        messageHolder.timeTv.setText(MEDIA_DOWNLOADING_TEXT);
                        if (messages.expiryType == VIEW_ONCE){
                            messageHolder.mediaIv.setVisibility(View.GONE);
                        } else if (messages.expiryType == VIEW_FOR_A_DAY){
                            messageHolder.mediaIv.setVisibility(View.VISIBLE);
                            Picasso.with(context).load(R.drawable.app_gradient).resize(imageWidth, imageHeight).centerCrop().into(messageHolder.mediaIv);
                        }
                        messageHolder.progressBar.setVisibility(View.VISIBLE);
                    } else if (status >= MEDIA_DOWNLOAD_COMPLETE) {
                        if (messages.memberId.equals(mFireBaseHelper.getMyUserId())) {
                            if (messages.mediaUploadingStatus == MEDIA_UPLOADING_FAILED) {
                                // uploading failed
                                messageHolder.progressBar.setVisibility(View.GONE);
                                messageHolder.timeTv.setText(SENDING_FAILED_TEXT);
                                messageHolder.messageDataFrame.setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View v) {
                                        messageHolder.progressBar.setVisibility(View.VISIBLE);
                                        messageHolder.timeTv.setText(MEDIA_UPLOADING_TEXT);
                                        // retry uploading of this media
                                        viewControlCallbacks.onUploadRetryClicked(messages.mediaId);
                                    }
                                });
                            } else if (messages.mediaUploadingStatus == MEDIA_UPLOADING_STARTED) {
                                messageHolder.timeTv.setText(MEDIA_UPLOADING_TEXT);
                                messageHolder.progressBar.setVisibility(View.VISIBLE);
                                if (messages.expiryType == VIEW_ONCE || messages.expiryType == QUICK_PEEK) {
                                    messageHolder.messageDataFrame.setOnClickListener(null);
                                    messageHolder.progressBar.setVisibility(View.GONE);
                                    messageHolder.mediaFrame.setVisibility(View.GONE);
                                    // handling quick peek and view once case
                                    if (messages.isVideo)
                                        messageHolder.stringTv.setText(context.getString(R.string.sent_media_text));
                                    else
                                        messageHolder.stringTv.setText(context.getString(R.string.sent_media_text));
                                } else if (messages.expiryType == VIEW_FOR_A_DAY || messages.expiryType == MESSAGE_EXPIRED_BUT_NOT_VIEWED) {
                                    messageHolder.mediaFrame.setVisibility(View.VISIBLE);
                                    if (!messages.isVideo) {
                                        Picasso.with(context).load(new File(messages.uri)).placeholder(R.drawable.app_gradient).resize(imageWidth, imageHeight).centerCrop().into(messageHolder.mediaIv);
                                        messageHolder.imageTask = null;
                                    } else {
                                        // show video thumbnail
                                        messageHolder.imageTask = new FetchImageTask(messages.uri, messageHolder.mediaIv);
                                        messageHolder.imageTask.execute();
                                    }
                                    messageHolder.messageDataFrame.setOnClickListener(new View.OnClickListener() {
                                        @Override
                                        public void onClick(View v) {
                                            chatViewController.onMediaClicked(messages);
                                        }
                                    });
                                }
                            } else if (messages.mediaUploadingStatus == MEDIA_UPLOADING_COMPLETE) {
                                messageHolder.timeTv.setText(AppLibrary.timeAccCurrentTime(messages.createdAt));
                                if (messages.expiryType == VIEW_ONCE || messages.expiryType == QUICK_PEEK) {
                                    messageHolder.messageDataFrame.setOnClickListener(null);
                                    messageHolder.progressBar.setVisibility(View.GONE);
                                    // handling quick peek and view once case
                                    if (messages.isVideo)
                                        messageHolder.stringTv.setText(context.getString(R.string.sent_media_text));
                                    else
                                        messageHolder.stringTv.setText(context.getString(R.string.sent_media_text));
                                    messageHolder.mediaFrame.setVisibility(View.GONE);
                                } else if (messages.expiryType == VIEW_FOR_A_DAY) {
                                    messageHolder.progressBar.setVisibility(View.GONE);
                                    messageHolder.mediaFrame.setVisibility(View.VISIBLE);
                                    if (!messages.isVideo) {
                                        Picasso.with(context).load(new File(messages.uri)).placeholder(R.drawable.app_gradient).resize(imageWidth, imageHeight).centerCrop().into(messageHolder.mediaIv);
                                        messageHolder.imageTask = null;
                                    } else {
                                        // show video thumbnail
                                        messageHolder.imageTask = new FetchImageTask(messages.uri, messageHolder.mediaIv);
                                        messageHolder.imageTask.execute();
                                    }
                                    messageHolder.messageDataFrame.setOnClickListener(new View.OnClickListener() {
                                        @Override
                                        public void onClick(View v) {
                                            chatViewController.onMediaClicked(messages);
                                        }
                                    });
                                }
                            }
                        } else {
                            if (messages.expiryType == VIEW_ONCE || messages.expiryType == QUICK_PEEK) {
                                messageHolder.progressBar.setVisibility(View.GONE);
                                // handling quick peek and view once case
                                if (messages.isVideo)
                                    messageHolder.stringTv.setText(context.getString(R.string.tap_to_view_video));
                                else
                                    messageHolder.stringTv.setText(context.getString(R.string.tap_to_view_image));
                                messageHolder.mediaFrame.setVisibility(View.GONE);
                                messageHolder.messageDataFrame.setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View v) {
                                        chatViewController.onMediaClicked(messages);
                                    }
                                });
                            } else if (messages.expiryType == VIEW_FOR_A_DAY || messages.expiryType == MESSAGE_EXPIRED_BUT_NOT_VIEWED) {
                                if (!messages.isVideo) {
                                    Picasso.with(context).load(new File(messages.uri)).placeholder(R.drawable.app_gradient).resize(imageWidth, imageHeight).centerCrop().into(messageHolder.mediaIv, new Callback() {
                                        @Override
                                        public void onSuccess() {
                                            messageHolder.progressBar.setVisibility(View.GONE);
                                            messageHolder.mediaFrame.setVisibility(View.VISIBLE);
                                        }

                                        @Override
                                        public void onError() {

                                        }
                                    });
                                    messageHolder.imageTask = null;
                                } else {
                                    // show video thumbnail
                                    messageHolder.imageTask = new FetchImageTask(messages.uri, messageHolder.mediaIv);
                                    messageHolder.imageTask.execute();
                                    messageHolder.progressBar.setVisibility(View.GONE);
                                    messageHolder.mediaFrame.setVisibility(View.VISIBLE);
                                }
                                messageHolder.messageDataFrame.setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View v) {
                                        chatViewController.onMediaClicked(messages);
                                    }
                                });
                            }
                        }
                        if (!messages.memberId.equals(mFireBaseHelper.getMyUserId()) && (messages.viewers == null || !messages.viewers.containsKey(mFireBaseHelper.getMyUserId()))) {
                            unseenDownloadedChatMediaMap.put(messages.messageId, messages);
                        }
                    }
                }
            } else {
                messageHolder.messageDataFrame.setOnClickListener(null);
                if (messages.memberId.equals(mFireBaseHelper.getMyUserId()) && messages.expiryType == VIEW_ONCE_AND_VIEWED) {
                    if (messages.viewers != null && !messages.viewers.isEmpty()) {
                        // opened view once message
                        if (messages.isVideo)
                            messageHolder.stringTv.setText(context.getString(R.string.opened_media_text));
                        else
                            messageHolder.stringTv.setText(context.getString(R.string.opened_media_text));
                    } else {
                        // handling quick peek and view once case
                        if (messages.isVideo)
                            messageHolder.stringTv.setText(context.getString(R.string.sent_media_text));
                        else
                            messageHolder.stringTv.setText(context.getString(R.string.sent_media_text));
                    }
                    messageHolder.progressBar.setVisibility(View.GONE);
                    messageHolder.mediaFrame.setVisibility(View.GONE);
                } else if (!messages.memberId.equals(mFireBaseHelper.getMyUserId()) && messages.expiryType == VIEW_ONCE_AND_VIEWED) {
                    if (messages.viewers != null && !messages.viewers.isEmpty()) {
                        // handling quick peek and view once case
                        if (messages.isVideo)
                            messageHolder.stringTv.setText(context.getString(R.string.opened_media_text));
                        else
                            messageHolder.stringTv.setText(context.getString(R.string.opened_media_text));
                    } else {
                        if (messages.isVideo)
                            messageHolder.stringTv.setText(context.getString(R.string.delivered_media_text));
                        else
                            messageHolder.stringTv.setText(context.getString(R.string.delivered_media_text));
                    }
                    messageHolder.mediaFrame.setVisibility(View.GONE);
                    messageHolder.progressBar.setVisibility(View.GONE);
                }
            }
        }
    }

    private Boolean deleteUnderProgress;
    private final int deleteColor = Color.parseColor("#800000FF");
    private final int normalColor = Color.parseColor("#00000000");
    private HashMap<String, Boolean> deletionListMap = new HashMap<>();

    public void clearDeletionList() {
        deletionListMap.clear();
        deleteUnderProgress = false;
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return (messagesLinkedHashMap == null ? 0 : messagesLinkedHashMap.size());
    }

    @Override
    public int getItemViewType(int position) {
        if (messagesLinkedHashMap == null || messagesLinkedHashMap.size() == 0)
            return VIEW_TYPE_NO_CHATS;
        if (getMessageByIndex(position).type == SYSTEM_MESSAGE)
            return VIEW_TYPE_SYSTEM_MESSAGE;
        return VIEW_TYPE_NORMAL;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public ViewHolder(View itemView) {
            super(itemView);
        }
    }

    public HashMap<String, Boolean> getDeletionListMap() {
        return deletionListMap;
    }

    class SystemMessageVH extends RecyclerView.ViewHolder {
        TextView systemMessageTv;

        public SystemMessageVH(View itemView) {
            super(itemView);
            systemMessageTv = (TextView) itemView.findViewById(R.id.system_messageTV);
        }
    }

    class MessageVH extends RecyclerView.ViewHolder {
        TextView senderNameTv;
        TextView stringTv, timeTv;
        FrameLayout messageDataFrame;
        LinearLayout rootView;// a linear layout
        ImageView mediaIv;
        FrameLayout mediaFrame;
        ProgressBar progressBar;
        FetchImageTask imageTask;

        public MessageVH(View itemView) {
            super(itemView);
            rootView = (LinearLayout) itemView;
            stringTv = (TextView) itemView.findViewById(R.id.chatStringTV);
            timeTv = (TextView) itemView.findViewById(R.id.updatedAtTV);
            messageDataFrame = (FrameLayout) itemView.findViewById(R.id.messageDataFrame);
//            messageDataFrame.getLayoutParams().width = (int) (AppLibrary.getDeviceParams((CameraActivity) context, "width") * 0.8);
            senderNameTv = (TextView) itemView.findViewById(R.id.sender_nameTV);
            mediaIv = (ImageView) itemView.findViewById(R.id.media_image_IV);
            mediaFrame = (FrameLayout) itemView.findViewById(R.id.mediaFrame);
            progressBar = (ProgressBar) itemView.findViewById(R.id.loader_Progress);

            if (roomType == FRIEND_ROOM)
                senderNameTv.setVisibility(View.GONE);
            rootView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (!deleteUnderProgress) return;//do something else
                    if (!isDeletionAllowed) return;
                    Log.d(TAG, "VHITem" + String.valueOf(getLayoutPosition()) + rootView.getTag());
                    RoomsModel.Messages message = (RoomsModel.Messages) rootView.getTag();
                    if (message == null) {
                        Log.d(TAG, " its a system message ignoring");
                        return;
                    }
                    Log.d(TAG, message.toString());
                    String messageId = message.messageId;
                    if (deletionListMap.get(messageId) == null)
                        deletionListMap.put(messageId, true);
                    else
                        deletionListMap.put(messageId, !deletionListMap.get(messageId));
                    notifyDataSetChanged();
                    deletionMapChanged();
                }
            });
            rootView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    Log.d(TAG, " on Long Click");
                    if (!isDeletionAllowed)
                        return false;
                    RoomsModel.Messages message = (RoomsModel.Messages) rootView.getTag();
                    if (message == null) {
                        Log.d(TAG, " its a system message ignoring");
                        return true;
                    }
                    String messageId = message.messageId;
                    if (deletionListMap.get(messageId) == null)
                        deletionListMap.put(messageId, true);
                    else
                        deletionListMap.put(messageId, !deletionListMap.get(messageId));
                    notifyDataSetChanged();
                    deleteUnderProgress = true;
                    deletionMapChanged();
                    return true;
                }
            });
        }

        private void deletionMapChanged() {
            int deletionCount = 0;
            for (Map.Entry<String, Boolean> entry : deletionListMap.entrySet()) {
                Log.d(TAG, " on deletionMapChanged " + entry.getKey() + " delete: " + entry.getValue());
                if (entry.getValue())
                    deletionCount++;
            }
            if (clickInterface != null)
                clickInterface.onItemClick(deletionCount, null);
        }
    }

    private class HeaderVH extends RecyclerView.ViewHolder {
        public HeaderVH(View itemView) {
            super(itemView);
        }
    }


    public interface ChatViewController {
        void onMediaClicked(RoomsModel.Messages message);
    }

    public interface ViewControlCallbacks {
        void onUploadRetryClicked(String mediaId);
    }

    private class FetchImageTask extends AsyncTask<String, Void, Bitmap> {

        String imageUrl;
        ImageView imageView;
        Bitmap bitmap;

        public FetchImageTask(String url, ImageView imageView) {
            this.imageUrl = url;
            this.imageView = imageView;
        }

        @SuppressLint("LongLogTag")
        @Override
        protected Bitmap doInBackground(String... params) {
            bitmap = ThumbnailUtils.createVideoThumbnail(new File(imageUrl).getAbsolutePath(), MediaStore.Images.Thumbnails.MINI_KIND);
            if (bitmap != null) {
                return Bitmap.createScaledBitmap(bitmap, imageWidth, imageHeight, false);
            } else {
                return null;
            }
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            super.onPostExecute(bitmap);
            if (bitmap != null)
                imageView.setImageBitmap(bitmap);
            else
                imageView.setImageResource(R.drawable.app_gradient);
        }


        @Override
        protected void onCancelled() {
            if (bitmap != null) {
                bitmap.recycle();
                bitmap = null;
            }
        }

        private void cleanup() {
            if (bitmap != null) {
                bitmap.recycle();
                bitmap = null;
            }
        }
    }
}
