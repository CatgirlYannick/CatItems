package dev.catgirlyannick.catitems.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SmallCapsFormatterTest {
    @Test
    void preservesFormattingPlaceholdersAndGermanCharacters() {
        assertEquals("<gold>ᴍʏꜱᴛɪꜱᴄʜᴇ ᴀᴜꜱʀüꜱᴛᴜɴɢ</gold> {amount}",
                SmallCapsFormatter.formatTemplate("<gold>Mystische Ausrüstung</gold> {amount}"));
    }
}
