package com.example.diplomaappmodeltflite;

public class SoundUtils {
    public static String mapDisplayNameToResourceName(String displayName) {
        switch (displayName) {
            case "До":
                return "do_note";
            case "Ре":
                return "re";
            case "Мі":
                return "mi";
            case "Фа":
                return "fa";
            case "Соль":
                return "sol";
            case "Ля":
                return "la";
            case "Сі":
                return "si";
            default:
                return null;
        }
    }
}
