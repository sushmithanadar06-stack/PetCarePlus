package com.example.petcareplus.adapters;

import android.content.Intent;
import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.petcareplus.R;
import com.example.petcareplus.activities.FullscreenPhotoActivity;
import com.example.petcareplus.models.AdoptionMessage;
import com.example.petcareplus.utils.ImageUtils;

import java.util.List;

public class AdoptionChatAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    public interface OnMessageLongClick {
        void onLongClick(AdoptionMessage message);
    }

    private static final int TYPE_SENT = 1;
    private static final int TYPE_RECEIVED = 2;

    private final List<AdoptionMessage> messages;
    private final String currentUid;
    private final OnMessageLongClick longClickListener;

    public AdoptionChatAdapter(List<AdoptionMessage> messages, String currentUid, OnMessageLongClick listener) {
        this.messages = messages;
        this.currentUid = currentUid;
        this.longClickListener = listener;
    }

    @Override
    public int getItemViewType(int position) {
        return messages.get(position).getSenderId().equals(currentUid) ? TYPE_SENT : TYPE_RECEIVED;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        int layout = viewType == TYPE_SENT ? R.layout.item_chat_message_sent : R.layout.item_chat_message_received;
        View v = LayoutInflater.from(parent.getContext()).inflate(layout, parent, false);
        return new MessageViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {

        AdoptionMessage message = messages.get(position);
        MessageViewHolder vh = (MessageViewHolder) holder;

        if (message.getText() != null && !message.getText().isEmpty()) {
            vh.txtMsgBubble.setVisibility(View.VISIBLE);
            vh.txtMsgBubble.setText(message.getText());
        } else {
            vh.txtMsgBubble.setVisibility(View.GONE);
        }

        if (message.isReply()) {
            vh.replyQuoteBlock.setVisibility(View.VISIBLE);
            vh.txtReplySenderName.setText(message.getReplyToSenderName());
            vh.txtReplyPreviewText.setText(message.getReplyToPreviewText());
        } else {
            vh.replyQuoteBlock.setVisibility(View.GONE);
        }

        String photo = message.getPhotoBase64();
        if (photo != null && ImageUtils.isBase64Image(photo)) {
            Bitmap bitmap = ImageUtils.base64ToBitmap(photo);
            if (bitmap != null) {
                vh.imgMsgPhoto.setVisibility(View.VISIBLE);
                vh.imgMsgPhoto.setImageBitmap(bitmap);

                vh.imgMsgPhoto.setOnClickListener(v -> {
                    Intent intent = new Intent(v.getContext(), FullscreenPhotoActivity.class);
                    intent.putExtra(FullscreenPhotoActivity.EXTRA_PHOTO_BASE64, photo);
                    v.getContext().startActivity(intent);
                });
            } else {
                vh.imgMsgPhoto.setVisibility(View.GONE);
                vh.imgMsgPhoto.setOnClickListener(null);
            }
        } else {
            vh.imgMsgPhoto.setVisibility(View.GONE);
            vh.imgMsgPhoto.setOnClickListener(null);
        }

        vh.itemView.setOnLongClickListener(v -> {
            if (longClickListener != null) {
                longClickListener.onLongClick(message);
                return true;
            }
            return false;
        });
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    static class MessageViewHolder extends RecyclerView.ViewHolder {
        TextView txtMsgBubble, txtReplySenderName, txtReplyPreviewText;
        ImageView imgMsgPhoto;
        LinearLayout replyQuoteBlock;

        MessageViewHolder(View itemView) {
            super(itemView);
            txtMsgBubble = itemView.findViewById(R.id.txtMsgBubble);
            imgMsgPhoto = itemView.findViewById(R.id.imgMsgPhoto);
            replyQuoteBlock = itemView.findViewById(R.id.replyQuoteBlock);
            txtReplySenderName = itemView.findViewById(R.id.txtReplySenderName);
            txtReplyPreviewText = itemView.findViewById(R.id.txtReplyPreviewText);
        }
    }
}