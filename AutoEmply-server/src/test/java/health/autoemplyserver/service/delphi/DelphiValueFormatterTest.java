package health.autoemplyserver.service.delphi;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class DelphiValueFormatterTest {

    private final DelphiValueFormatter formatter = new DelphiValueFormatter();

    @Test
    void encodeStringWrapsPlainAsciiInQuotes() {
        assertThat(formatter.encodeString("Hello")).isEqualTo("'Hello'");
    }

    @Test
    void encodeStringDoublesEmbeddedSingleQuotes() {
        assertThat(formatter.encodeString("It's")).isEqualTo("'It''s'");
    }

    @Test
    void encodeStringEncodesNonAsciiCharactersAsCharCodes() {
        assertThat(formatter.encodeString("설명")).isEqualTo("#49444#47749");
        assertThat(formatter.encodeString("A설B")).isEqualTo("'A'#49444'B'");
    }

    @Test
    void encodeStringReturnsEmptyLiteralForNullAndEmpty() {
        assertThat(formatter.encodeString(null)).isEqualTo("''");
        assertThat(formatter.encodeString("")).isEqualTo("''");
    }

    @Test
    void encodePascalStringLiteralsReencodesKoreanLiteralsInsideSource() {
        assertThat(formatter.encodePascalStringLiterals("ShowMessage('설명');"))
            .isEqualTo("ShowMessage(#49444#47749);");
    }

    @Test
    void encodePascalStringLiteralsKeepsEscapedQuotesAndUnterminatedLiterals() {
        assertThat(formatter.encodePascalStringLiterals("S := 'It''s ok';"))
            .isEqualTo("S := 'It''s ok';");
        assertThat(formatter.encodePascalStringLiterals("x := 'abc"))
            .isEqualTo("x := 'abc");
    }

    @Test
    void pixelsToMmConvertsWithHalfUpRoundingAtRequestedScale() {
        assertThat(formatter.pixelsToMm(0, 2)).isEqualTo("0.00");
        assertThat(formatter.pixelsToMm(10, 2)).isEqualTo("26.46");
        assertThat(formatter.pixelsToMm(1, 12)).isEqualTo("2.645833333333");
    }

    @Test
    void toDelphiColorConvertsWebHexRgbToDelphiBgr() {
        assertThat(formatter.toDelphiColor("#FF8000", "clBlack")).isEqualTo("$000080FF");
        assertThat(formatter.toDelphiColor("#00FF00", "clBlack")).isEqualTo("$0000FF00");
    }

    @Test
    void toDelphiColorPassesThroughDelphiNativeFormats() {
        assertThat(formatter.toDelphiColor("clBtnFace", "clBlack")).isEqualTo("clBtnFace");
        assertThat(formatter.toDelphiColor("$00112233", "clBlack")).isEqualTo("$00112233");
    }

    @Test
    void toDelphiColorFallsBackForBlankOrUnparsableValues() {
        assertThat(formatter.toDelphiColor(null, "clBlack")).isEqualTo("clBlack");
        assertThat(formatter.toDelphiColor("  ", "clWhite")).isEqualTo("clWhite");
        assertThat(formatter.toDelphiColor("#FFF", "clBlack")).isEqualTo("clBlack");
        assertThat(formatter.toDelphiColor("112233", "clBlack")).isEqualTo("clBlack");
    }

    @Test
    void toDelphiColorDropsLeadingPairOfEightDigitWebHex() {
        // Current behavior: a 9-char "#XXRRGGBB" value loses its first hex pair before conversion.
        assertThat(formatter.toDelphiColor("#CC123456", "clBlack")).isEqualTo("$00563412");
    }

    @Test
    void mapAlignmentTranslatesKnownValuesAndDefaultsToLeft() {
        assertThat(formatter.mapAlignment(null)).isEqualTo("taLeftJustify");
        assertThat(formatter.mapAlignment("  Center ")).isEqualTo("taCenter");
        assertThat(formatter.mapAlignment("RIGHT")).isEqualTo("taRightJustify");
        assertThat(formatter.mapAlignment("justify")).isEqualTo("taLeftJustify");
    }

    @Test
    void ensureSemicolonAppendsOnlyWhenMissing() {
        assertThat(formatter.ensureSemicolon(null)).isEmpty();
        assertThat(formatter.ensureSemicolon("   ")).isEmpty();
        assertThat(formatter.ensureSemicolon("procedure Foo")).isEqualTo("procedure Foo;");
        assertThat(formatter.ensureSemicolon("procedure Foo;")).isEqualTo("procedure Foo;");
    }

    @Test
    void buildImplementationDeclarationQualifiesMethodWithClassPrefix() {
        assertThat(formatter.buildImplementationDeclaration("procedure BuildReport", "FormX"))
            .isEqualTo("procedure TFormX.BuildReport;");
        assertThat(formatter.buildImplementationDeclaration("function CalcValue: Integer", "FormX"))
            .isEqualTo("function TFormX.CalcValue: Integer;");
    }

    @Test
    void buildImplementationDeclarationHandlesBareQualifiedAndBlankDeclarations() {
        // A bare name is promoted to a procedure; an already-qualified declaration is untouched.
        assertThat(formatter.buildImplementationDeclaration("DoStuff", "FormX"))
            .isEqualTo("procedure TFormX.DoStuff;");
        assertThat(formatter.buildImplementationDeclaration("procedure TFormX.Run", "FormX"))
            .isEqualTo("procedure TFormX.Run;");
        assertThat(formatter.buildImplementationDeclaration("   ", "FormX")).isEmpty();
    }
}
