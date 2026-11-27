package com.hesselberg.sudokusolver;

import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

/**
 * Persists the on-screen board and clue flags across process death
 * (rotation still uses the activity instance state).
 */
public final class BoardStore {

    static final String PREFS_NAME = "sudoku_board";
    private static final String KEY_BOARD = "board";
    private static final String KEY_CLUES = "clues";

    private BoardStore() {
    }

    public static void save(
            @NonNull SharedPreferences prefs,
            @NonNull String board,
            @NonNull boolean[] clues) {
        if (board.length() != Board.DIM * Board.DIM || clues.length != Board.DIM * Board.DIM) {
            return;
        }
        prefs.edit()
                .putString(KEY_BOARD, board)
                .putString(KEY_CLUES, encodeClues(clues))
                .apply();
    }

    public static void clear(@NonNull SharedPreferences prefs) {
        prefs.edit().remove(KEY_BOARD).remove(KEY_CLUES).apply();
    }

    /**
     * @return the saved board, or null if nothing valid is stored
     */
    @Nullable
    public static String loadBoard(@NonNull SharedPreferences prefs) {
        String board = prefs.getString(KEY_BOARD, null);
        if (board == null || board.length() != Board.DIM * Board.DIM) {
            return null;
        }
        return board;
    }

    /**
     * @return clue flags when valid, otherwise null
     */
    @Nullable
    public static boolean[] loadClues(@NonNull SharedPreferences prefs) {
        String encoded = prefs.getString(KEY_CLUES, null);
        return decodeClues(encoded);
    }

    @VisibleForTesting
    @NonNull
    static String encodeClues(@NonNull boolean[] clues) {
        char[] chars = new char[clues.length];
        for (int i = 0; i < clues.length; i++) {
            chars[i] = clues[i] ? '1' : '0';
        }
        return new String(chars);
    }

    @VisibleForTesting
    @Nullable
    static boolean[] decodeClues(@Nullable String encoded) {
        if (encoded == null || encoded.length() != Board.DIM * Board.DIM) {
            return null;
        }
        boolean[] clues = new boolean[encoded.length()];
        for (int i = 0; i < encoded.length(); i++) {
            char c = encoded.charAt(i);
            if (c != '0' && c != '1') {
                return null;
            }
            clues[i] = c == '1';
        }
        return clues;
    }
}
