package l1j.server.server.clientpackets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.ByteArrayOutputStream;
import java.nio.charset.Charset;
import java.util.Locale;

import org.junit.jupiter.api.Test;

class LoginRequestTest {
    @Test
    void parsesWrappedLoginAndPreservesPassword() {
        LoginRequest request = LoginRequest.parse(
                packet(210, true, "PLAYER", " PaSsword! ", "MS950"), "MS950");

        assertNotNull(request);
        assertEquals("player", request.getAccountName());
        assertEquals(" PaSsword! ", request.getPassword());
    }

    @Test
    void parsesDirectLoginWithOpaqueNonzeroSuffix() {
        byte[] packet = packet(119, false, "test1234", "Test1234", "MS950");
        // Observed legacy launcher: two eight-byte strings, seven DWORDs, padding.
        byte[] padded = new byte[48];
        System.arraycopy(packet, 0, padded, 0, packet.length);
        padded[packet.length] = 0x7f;
        padded[packet.length + 3] = 0x01;
        padded[packet.length + 24] = 0x1f;

        LoginRequest request = LoginRequest.parse(padded, "MS950");

        assertNotNull(request);
        assertEquals("test1234", request.getAccountName());
        assertEquals("Test1234", request.getPassword());
    }

    @Test
    void keepsBig5AccountAndPasswordForWrappedLogin() {
        LoginRequest request = LoginRequest.parse(
                packet(210, true, "玩家", " 密碼 ! ", "MS950"), "MS950");

        assertNotNull(request);
        assertEquals("玩家", request.getAccountName());
        assertEquals(" 密碼 ! ", request.getPassword());
    }

    @Test
    void directLoginCanKeepBig5Password() {
        LoginRequest request = LoginRequest.parse(
                packet(119, false, "Player", "密碼", "Big5"), "Big5");

        assertNotNull(request);
        assertEquals("密碼", request.getPassword());
    }

    @Test
    void lowercasesAccountIndependentlyOfDefaultLocale() {
        Locale previous = Locale.getDefault();
        try {
            Locale.setDefault(Locale.forLanguageTag("tr-TR"));
            LoginRequest request = LoginRequest.parse(
                    packet(119, false, "I", "I", "MS950"), "MS950");

            assertNotNull(request);
            assertEquals("i", request.getAccountName());
            assertEquals("I", request.getPassword());
        } finally {
            Locale.setDefault(previous);
        }
    }

    @Test
    void ignoresUnsupportedActionsInsteadOfParsingTheirTrailingCredentials() {
        for (int action : new int[] {0x0b, 0x1c, 0x01, 0x07, 0x1f, 0x7f}) {
            byte[] packet = packet(210, true, "Player", "Pass", "MS950");
            packet[1] = (byte) action;
            assertNull(LoginRequest.parse(packet, "MS950"));
        }
        assertNull(LoginRequest.parse(packet(210, false, "Player", "Pass", "MS950"), "MS950"));
    }

    @Test
    void rejectsTruncatedAndEmptyFields() {
        assertNull(LoginRequest.parse(null, "MS950"));
        assertNull(LoginRequest.parse(new byte[0], "MS950"));
        assertNull(LoginRequest.parse(new byte[] {(byte) 210}, "MS950"));
        assertNull(LoginRequest.parse(new byte[] {(byte) 210, 0x06}, "MS950"));
        assertNull(LoginRequest.parse(new byte[] {(byte) 210, 'a'}, "MS950"));
        assertNull(LoginRequest.parse(new byte[] {(byte) 210, 'a', 0}, "MS950"));
        assertNull(LoginRequest.parse(new byte[] {(byte) 210, 'a', 0, 'b'}, "MS950"));
        for (boolean wrapped : new boolean[] {true, false}) {
            int opcode = wrapped ? 210 : 119;
            assertNull(LoginRequest.parse(packet(opcode, wrapped, "", "Pass", "MS950"), "MS950"));
            assertNull(LoginRequest.parse(packet(opcode, wrapped, "Player", "", "MS950"), "MS950"));
        }
    }

    @Test
    void rejectsInvalidCharsetsAndMalformedMultibyteSequences() {
        byte[] packet = packet(210, true, "Player", "Pass", "MS950");
        assertNull(LoginRequest.parse(packet, "not-a-real-charset"));
        assertNull(LoginRequest.parse(packet, null));
        assertNull(LoginRequest.parse(
                new byte[] {(byte) 210, 0x06, (byte) 0x81, 0, 'p', 0}, "MS950"));
        assertNull(LoginRequest.parse(
                new byte[] {(byte) 210, 0x06, 'a', 0, (byte) 0x81, 0}, "MS950"));
    }

    @Test
    void rejectsControlCharactersAndNonAsciiDirectAccounts() {
        assertNull(LoginRequest.parse(packet(119, false, "Player Name", "Pass", "MS950"), "MS950"));
        assertNull(LoginRequest.parse(packet(119, false, "玩家", "Pass", "MS950"), "MS950"));
        for (boolean wrapped : new boolean[] {true, false}) {
            int opcode = wrapped ? 210 : 119;
            assertNull(LoginRequest.parse(packet(opcode, wrapped, "Player\t", "Pass", "UTF-8"), "UTF-8"));
            assertNull(LoginRequest.parse(packet(opcode, wrapped, "Player", "Pa\nss", "UTF-8"), "UTF-8"));
            assertNull(LoginRequest.parse(packet(opcode, wrapped, "Player", "Pa\u0085ss", "UTF-8"), "UTF-8"));
            assertNull(LoginRequest.parse(packet(opcode, wrapped, "Player", "Pa\u200ess", "UTF-8"), "UTF-8"));
        }
    }

    @Test
    void limitsAccountLengthWithoutImposingAnUnspecifiedPasswordLimit() {
        for (boolean wrapped : new boolean[] {true, false}) {
            int opcode = wrapped ? 210 : 119;
            assertNull(LoginRequest.parse(packet(opcode, wrapped, "a".repeat(51), "Pass", "MS950"), "MS950"));
            LoginRequest request = LoginRequest.parse(
                    packet(opcode, wrapped, "a".repeat(50), "p".repeat(100), "MS950"), "MS950");
            assertNotNull(request);
            assertEquals("p".repeat(100), request.getPassword());
        }
    }

    @Test
    void rejectsOtherOpcodes() {
        assertNull(LoginRequest.parse(packet(1, true, "Player", "Pass", "MS950"), "MS950"));
    }

    private static byte[] packet(int opcode, boolean wrapped, String account,
            String password, String charsetName) {
        Charset charset = Charset.forName(charsetName);
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        bytes.write(opcode);
        if (wrapped) {
            bytes.write(0x06);
        }
        byte[] accountBytes = account.getBytes(charset);
        bytes.write(accountBytes, 0, accountBytes.length);
        bytes.write(0);
        byte[] passwordBytes = password.getBytes(charset);
        bytes.write(passwordBytes, 0, passwordBytes.length);
        bytes.write(0);
        return bytes.toByteArray();
    }
}
