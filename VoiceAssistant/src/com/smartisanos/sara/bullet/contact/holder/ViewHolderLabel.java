package com.smartisanos.sara.bullet.contact.holder;

import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.smartisanos.sara.R;
import com.smartisanos.sara.bullet.contact.adapter.PickContactAdapter;
import com.smartisanos.sara.bullet.contact.model.AbsContactItem;
import com.smartisanos.sara.bullet.contact.model.LabelItem;

public class ViewHolderLabel extends ViewHolderBase {

    private ImageView labelIcon;
    private TextView nameTextView;

    public ViewHolderLabel(View itemView) {
        super(itemView);
    }

    protected void inflate(View view) {
        labelIcon = (ImageView) itemView.findViewById(R.id.label_icon);
        nameTextView = (TextView) itemView.findViewById(R.id.tv_nickname);
    }

    @Override
    public void refreshView(PickContactAdapter adapter, AbsContactItem item) {
        LabelItem c = (LabelItem)item ;
        int labelRes = c.getLabRes();
        if(labelRes > 0) {
            labelIcon.setBackgroundResource(labelRes);
            labelIcon.setVisibility(View.VISIBLE);
        } else {
            labelIcon.setVisibility(View.GONE);
        }
        nameTextView.setText(c.getText());
    }
}
