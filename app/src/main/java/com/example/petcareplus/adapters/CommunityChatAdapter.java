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
import com.example.petcareplus.activities.PhotoViewerActivity;
import com.example.petcareplus.models.ChatMessage;
import com.example.petcareplus.utils.ImageUtils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class CommunityChatAdapter extends RecyclerView.Adapter<CommunityChatAdapter.MessageViewHolder> {

    public interface OnMessageLongClick {
        void onLongClick(ChatMessage message);
    }

    private final List<ChatMessage> messages;
    private final String currentUid;
    private final OnMessageLongClick longClickListener;

    public CommunityChatAdapter(List<ChatMessage> messages, String currentUid, OnMessageLongClick listener) {
        this.messages = messages;
        this.currentUid = currentUid;
        this.longClickListener = listener;
    }

    @NonNull
    @Override
    public MessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_chat_message, parent, false);
        return new MessageViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull MessageViewHolder holder, int position) {

        ChatMessage msg = messages.get(position);
        boolean isMine = currentUid.equals(msg.getSenderId());

        holder.txtSenderName.setText(isMine ? "You" : msg.getSenderName());

        SimpleDateFormat timeFormat = new SimpleDateFormat("MMM dd, h:mm a", Locale.getDefault());
        holder.txtMessageTime.setText(timeFormat.format(new Date(msg.getTimestampMillis())));

        // No maxLines, no ellipsize — the full message text is always set
        // and always shown, however long it is. It will wrap onto as many
        // lines as it needs inside the bubble.
        if (msg.getText() != null && !msg.getText().trim().isEmpty()) {
            holder.txtMessageText.setVisibility(View.VISIBLE);
            holder.txtMessageText.setText(msg.getText());
        } else {
            holder.txtMessageText.setVisibility(View.GONE);
        }

        if (msg.isReply()) {
            holder.replyQuoteBlock.setVisibility(View.VISIBLE);
            holder.txtReplyQuoteSender.setText(msg.getReplyToSenderName());
            holder.txtReplyQuoteText.setText(msg.getReplyToPreviewText());
        } else {
            holder.replyQuoteBlock.setVisibility(View.GONE);
        }

        String photo = msg.getPhotoBase64();
        if (photo != null && ImageUtils.isBase64Image(photo)) {
            Bitmap bitmap = ImageUtils.base64ToBitmap(photo);
            if (bitmap != null) {
                holder.imgMessagePhoto.setVisibility(View.VISIBLE);
                holder.imgMessagePhoto.setImageBitmap(bitmap);

                holder.imgMessagePhoto.setOnClickListener(v -> {
                    Intent intent = new Intent(v.getContext(), PhotoViewerActivity.class);
                    intent.putExtra("photoBase64", photo);
                    v.getContext().startActivity(intent);
                });
            } else {
                holder.imgMessagePhoto.setVisibility(View.GONE);
                holder.imgMessagePhoto.setOnClickListener(null);
            }
        } else {
            holder.imgMessagePhoto.setVisibility(View.GONE);
            holder.imgMessagePhoto.setOnClickListener(null);
        }

        // Safe now: messageBubble is a CHILD inside the item's real root
        // (see item_chat_message.xml), so it genuinely has
        // LinearLayout.LayoutParams and re-measures correctly on every
        // bind — including when a recycled holder switches from a short
        // message to a long one.
        LinearLayout.LayoutParams params =
                (LinearLayout.LayoutParams) holder.messageBubble.getLayoutParams();
        params.gravity = isMine ? android.view.Gravity.END : android.view.Gravity.START;
        holder.messageBubble.setLayoutParams(params);

        holder.itemView.setOnLongClickListener(v -> {
            if (longClickListener != null) {
                longClickListener.onLongClick(msg);
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
        LinearLayout messageBubble, replyQuoteBlock;
        TextView txtSenderName, txtMessageText, txtMessageTime, txtReplyQuoteSender, txtReplyQuoteText;
        ImageView imgMessagePhoto;

        MessageViewHolder(View itemView) {
            super(itemView);
            messageBubble = itemView.findViewById(R.id.messageBubble);
            txtSenderName = itemView.findViewById(R.id.txtSenderName);
            txtMessageText = itemView.findViewById(R.id.txtMessageText);
            txtMessageTime = itemView.findViewById(R.id.txtMessageTime);
            imgMessagePhoto = itemView.findViewById(R.id.imgMessagePhoto);
            replyQuoteBlock = itemView.findViewById(R.id.replyQuoteBlock);
            txtReplyQuoteSender = itemView.findViewById(R.id.txtReplyQuoteSender);
            txtReplyQuoteText = itemView.findViewById(R.id.txtReplyQuoteText);
        }
    }
}