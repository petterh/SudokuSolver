package com.hesselberg.sudokusolver;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class MainActivityEditTest {

    @Test
    public void nextClueValue_cyclesBlankThroughDigits() {
        assertEquals('1', MainActivity.nextClueValue(' '));
        assertEquals('2', MainActivity.nextClueValue('1'));
        assertEquals('9', MainActivity.nextClueValue('8'));
        assertEquals(' ', MainActivity.nextClueValue('9'));
        assertEquals('1', MainActivity.nextClueValue('0'));
    }
}
