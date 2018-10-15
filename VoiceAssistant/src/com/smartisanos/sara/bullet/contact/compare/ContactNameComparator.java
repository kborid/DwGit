package com.smartisanos.sara.bullet.contact.compare;

import com.smartisanos.sara.bullet.contact.model.ContactItem;
import com.smartisanos.sara.bullet.util.TextComparatorUtils;

import java.util.Comparator;

public class ContactNameComparator implements Comparator<ContactItem> {

    @Override
    public int compare(ContactItem o1, ContactItem o2) {
        // first letter > friend/team > name
        int result = TextComparatorUtils.compareIgnoreCase(TextComparatorUtils.getLeadingChar(o1.getPinyin()), TextComparatorUtils.getLeadingChar(o2.getPinyin()));
        if (result != 0) {
            return result;
        }

        result = o1.getOrderByMessageType() - o2.getOrderByMessageType();
        if (result != 0) {
            return result;
        }

        return TextComparatorUtils.compareIgnoreCase(o1.getPinyin(), o2.getPinyin());
    }
}
