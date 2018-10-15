package com.smartisanos.sara.bubble.manager;

import com.smartisanos.sara.R;
import com.smartisanos.sara.util.SaraUtils;

import android.service.onestep.GlobalBubble;

public class BubbleThemeManager {
    public static final int BACKGROUND_BUBBLE_LARGE = 0;
    public static final int BACKGROUND_BUBBLE_NORMAL = 1;
    public static final int BACKGROUND_BUBBLE_ARROW = 2;
    public static final int BACKGROUND_BUBBLE_ARROW_lARGE = 3;
    public static final int BACKGROUND_BUBBLE_TEXT_COLOR = 4;
    public static final int BACKGROUND_BUBBLE_BOOM_ICON = 5;
    public static final int BACKGROUND_BUBBLE_ATTACHMENT_ICON = 6;
    public static final int BACKGROUND_BUBBLE_CALENDAR_ICON = 7;
    public static final int BACKGROUND_BUBBLE_SHARE_ICON = 8;
    public static final int BACKGROUND_BUBBLE_INSERT_ICON = 9;
    public static final int BACKGROUND_BUBBLE_PLAER_ICON = 10;
    public static final int BACKGROUND_BUBBLE_PAUSE_ICON = 11;
    public static final int BACKGROUND_BUBBLE_CURSOR_ICON = 12;
    public static final int BACKGROUND_BUBBLE_PLAY_ICON = 13;
    public static final int BACKGROUND_UNFOLD_REMIND_ICON = 14;
    public static final int BACKGROUND_FOLD_REMIND_ICON = 15;

    public static int getBackgroudRes(int color, int type) {
        if (type == BACKGROUND_BUBBLE_LARGE) {
            switch (color) {
                case GlobalBubble.COLOR_RED: {
                    return R.drawable.pop_expansion_bg_red;
                }
                case GlobalBubble.COLOR_ORANGE: {
                    return R.drawable.pop_expansion_bg_orange;
                }
                case GlobalBubble.COLOR_GREEN: {
                    return R.drawable.pop_expansion_bg_green;
                }
                case GlobalBubble.COLOR_PURPLE: {
                    return R.drawable.pop_expansion_bg_purple;
                }
                case GlobalBubble.COLOR_NAVY_BLUE: {
                    return R.drawable.ppt_pop_expansion_bg;
                }
                case GlobalBubble.COLOR_SHARE: {
                    return R.drawable.pop_expansion_bg_share;
                }
                default: {
                    return R.drawable.pop_expansion_bg;
                }
            }
        } else if (type == BACKGROUND_BUBBLE_NORMAL) {
            switch (color) {
                case GlobalBubble.COLOR_RED: {
                    return R.drawable.text_popup_red;
                }
                case GlobalBubble.COLOR_ORANGE: {
                    return R.drawable.text_popup_orange;
                }
                case GlobalBubble.COLOR_GREEN: {
                    return R.drawable.text_popup_green;
                }
                case GlobalBubble.COLOR_PURPLE: {
                    return R.drawable.text_popup_purple;
                }
                case GlobalBubble.COLOR_NAVY_BLUE: {
                    return R.drawable.ppt_text_popup;
                }
                case GlobalBubble.COLOR_SHARE: {
                    return R.drawable.text_popup_share;
                }
                default: {
                    return R.drawable.text_popup;
                }
            }
        } else if (type == BACKGROUND_BUBBLE_ARROW) {
            return getWaveArrowRes(color, SaraUtils.isLeftPopBubble() || SaraUtils.isBlindMode());
        } else if (type == BACKGROUND_BUBBLE_ARROW_lARGE) {
            return getWaveLargeArrowRes(color);
        } else if (type == BACKGROUND_BUBBLE_TEXT_COLOR) {
            switch (color) {
                case GlobalBubble.COLOR_SHARE: {
                    return R.color.share_text_color;
                }
                default: {
                    return R.color.color_wave;
                }
            }
        } else if (type == BACKGROUND_BUBBLE_BOOM_ICON) {
            switch (color) {
                case GlobalBubble.COLOR_SHARE: {
                    return R.drawable.bubble_btn_bang_share;
                }
                default: {
                    return R.drawable.bubble_btn_bang;
                }
            }
        } else if (type == BACKGROUND_BUBBLE_ATTACHMENT_ICON) {
            switch (color) {
                case GlobalBubble.COLOR_SHARE: {
                    return R.drawable.bubble_btn_attach_share;
                }
                default: {
                    return R.drawable.bubble_btn_attach;
                }
            }
        } else if (type == BACKGROUND_BUBBLE_CALENDAR_ICON) {
            switch (color) {
                case GlobalBubble.COLOR_SHARE: {
                    return R.drawable.bubble_btn_calendar_share;
                }
                default: {
                    return R.drawable.bubble_btn_calendar;
                }
            }
        } else if (type == BACKGROUND_BUBBLE_SHARE_ICON) {
            switch (color) {
                case GlobalBubble.COLOR_SHARE: {
                    return R.drawable.pop_share_share;
                }
                default: {
                    return R.drawable.pop_share;
                }
            }
        } else if (type == BACKGROUND_BUBBLE_INSERT_ICON) {
            switch (color) {
                case GlobalBubble.COLOR_SHARE: {
                    return R.drawable.save_to_chip_list_share;
                }
                default: {
                    return R.drawable.save_to_chip_list;
                }
            }
        } else if (type == BACKGROUND_BUBBLE_PLAER_ICON) {
            switch (color) {
                case GlobalBubble.COLOR_SHARE: {
                    return R.drawable.bubble_btn_player_share;
                }
                default: {
                    return R.drawable.bubble_btn_player;
                }
            }
        } else if (type == BACKGROUND_BUBBLE_PAUSE_ICON) {
            switch (color) {
                case GlobalBubble.COLOR_SHARE: {
                    return R.drawable.bubble_btn_pause_share;
                }
                default: {
                    return R.drawable.bubble_btn_pause;
                }
            }
        } else if (type == BACKGROUND_BUBBLE_CURSOR_ICON) {
            switch (color) {
                case GlobalBubble.COLOR_SHARE: {
                    return R.drawable.bubble_cursor_share;
                }
                default: {
                    return R.drawable.bubble_cursor;
                }
            }
        } else if (type == BACKGROUND_BUBBLE_PLAY_ICON) {
            switch (color) {
                case GlobalBubble.COLOR_SHARE: {
                    return R.drawable.play_icon_share;
                }
                default: {
                    return R.drawable.play_icon;
                }
            }
        } else if (type == BACKGROUND_UNFOLD_REMIND_ICON) {
            switch (color) {
                case GlobalBubble.COLOR_SHARE: {
                    return R.drawable.unfold_remind_icon_share;
                }
                default: {
                    return R.drawable.unfold_remind_icon;
                }
            }
        } else if (type == BACKGROUND_FOLD_REMIND_ICON) {
            switch (color) {
                case GlobalBubble.COLOR_SHARE: {
                    return R.drawable.little_remind_icon_share;
                }
                default: {
                    return R.drawable.little_remind_icon;
                }
            }
        }
        return 0;
    }

    public static int getWaveArrowRes(int color, boolean isLeftPopBubble) {
        switch (color) {
            case GlobalBubble.COLOR_RED: {
                return isLeftPopBubble ? R.drawable.popup_bottom_arrow_red_left : R.drawable.popup_bottom_arrow_red;
            }
            case GlobalBubble.COLOR_ORANGE: {
                return isLeftPopBubble ? R.drawable.popup_bottom_arrow_orange_left : R.drawable.popup_bottom_arrow_orange;
            }
            case GlobalBubble.COLOR_GREEN: {
                return isLeftPopBubble ? R.drawable.popup_bottom_arrow_green_left : R.drawable.popup_bottom_arrow_green;
            }
            case GlobalBubble.COLOR_PURPLE: {
                return isLeftPopBubble ? R.drawable.popup_bottom_arrow_purple_left : R.drawable.popup_bottom_arrow_purple;
            }
            case GlobalBubble.COLOR_NAVY_BLUE: {
                return isLeftPopBubble ? R.drawable.ppt_popup_bottom_arrow_left : R.drawable.ppt_popup_bottom_arrow;
            }
            case GlobalBubble.COLOR_SHARE: {
                return isLeftPopBubble ? R.drawable.popup_bottom_arrow_left : R.drawable.popup_bottom_arrow_share;
            }
            default: {
                return isLeftPopBubble ? R.drawable.popup_bottom_arrow_left : R.drawable.popup_bottom_arrow;
            }
        }
    }

    public static int getWaveLargeArrowRes(int color) {
        switch (color) {
            case GlobalBubble.COLOR_RED: {
                return R.drawable.popup_bottom_arrow_red_large;
            }
            case GlobalBubble.COLOR_ORANGE: {
                return R.drawable.popup_bottom_arrow_orange_large;
            }
            case GlobalBubble.COLOR_GREEN: {
                return R.drawable.popup_bottom_arrow_green_large;
            }
            case GlobalBubble.COLOR_PURPLE: {
                return R.drawable.popup_bottom_arrow_purple_large;
            }
            case GlobalBubble.COLOR_NAVY_BLUE: {
                return R.drawable.ppt_popup_bottom_arrow_large;
            }
            default: {
                return R.drawable.popup_bottom_arrow_large;
            }
        }
    }

    public static int getWaveColor(int color) {
        switch (color) {
            case GlobalBubble.COLOR_RED: {
                return R.color.bubble_wave_line_text_normal_red_color;
            }
            case GlobalBubble.COLOR_ORANGE: {
                return R.color.bubble_wave_line_text_normal_orange_color;
            }
            case GlobalBubble.COLOR_GREEN: {
                return R.color.bubble_wave_line_text_normal_green_color;
            }
            case GlobalBubble.COLOR_PURPLE: {
                return R.color.bubble_wave_line_text_normal_purple_color;
            }
            default: {
                return R.color.bubble_wave_line_text_normal_color;
            }
        }
    }

    public static int getWaveNomalColor(int color) {
        switch (color) {
            case GlobalBubble.COLOR_SHARE: {
                return R.color.bubble_wave_line_text_normal_share_color;
            }
            default: {
                return R.color.bubble_wave_line_text_color;
            }
        }
    }

    public static int getWavePassColor(int color) {
        switch (color) {
            case GlobalBubble.COLOR_SHARE: {
                return R.color.bubble_wave_line_text_share_color_pass;
            }
            default: {
                return R.color.bubble_wave_line_text_color_pass;
            }
        }
    }


}
