package io.github.lefpap.morningbro.message;

/**
 * Flag emoji from an ISO 3166-1 alpha-2 code: each letter becomes its regional indicator symbol.
 */
final class CountryFlag {

    private static final int REGIONAL_INDICATOR_A = 0x1F1E6;

    private CountryFlag() {
    }

    /**
     * Returns the flag for {@code countryCode}, which is assumed to be already validated as two uppercase letters.
     */
    static String of(String countryCode) {
        StringBuilder flag = new StringBuilder();
        countryCode.chars().forEach(letter -> flag.appendCodePoint(REGIONAL_INDICATOR_A + letter - 'A'));
        return flag.toString();
    }

}
