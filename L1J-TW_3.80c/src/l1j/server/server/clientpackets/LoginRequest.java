package l1j.server.server.clientpackets;

import static l1j.server.server.Opcodes.C_OPCODE_BEANFUNLOGINPACKET;
import static l1j.server.server.Opcodes.C_OPCODE_LOGINPACKET;

import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CodingErrorAction;
import java.util.Locale;

/** Parses supported login layouts without changing connection or account state. */
final class LoginRequest {
    private static final int LOGIN_ACTION = 0x06;
    private static final int MAX_ACCOUNT_LENGTH = 50;

    private final String accountName;
    private final String password;

    private LoginRequest(String accountName, String password) {
        this.accountName = accountName;
        this.password = password;
    }

    static LoginRequest parse(byte[] packet, String charsetName) {
        if (packet == null || packet.length < 2) {
            return null;
        }
        int opcode = packet[0] & 0xff;
        if (opcode != C_OPCODE_LOGINPACKET && opcode != C_OPCODE_BEANFUNLOGINPACKET) {
            return null;
        }

        int action = packet[1] & 0xff;
        if (action == 0x0b || action == 0x1c) {
            return null;
        }
        boolean wrapped = action == LOGIN_ACTION;
        // The legacy launcher uses opcode 0x77 and two strings followed by metadata.
        // Beanfun's other action packets must never be treated as credentials.
        if (!wrapped && opcode != C_OPCODE_LOGINPACKET) {
            return null;
        }
        int accountStart = wrapped ? 2 : 1;
        int accountEnd = findTerminator(packet, accountStart);
        if (accountEnd <= accountStart) {
            return null;
        }
        if (!wrapped && !isDirectAccount(packet, accountStart, accountEnd)) {
            return null;
        }
        int passwordStart = accountEnd + 1;
        int passwordEnd = findTerminator(packet, passwordStart);
        if (passwordEnd <= passwordStart) {
            return null;
        }

        try {
            Charset charset = Charset.forName(charsetName);
            String account = decode(packet, accountStart, accountEnd, charset);
            String password = decode(packet, passwordStart, passwordEnd, charset);
            if (account.isEmpty() || account.length() > MAX_ACCOUNT_LENGTH
                    || password.isEmpty() || containsControl(account)
                    || containsControl(password)) {
                return null;
            }
            return new LoginRequest(account.toLowerCase(Locale.ROOT), password);
        } catch (IllegalArgumentException | CharacterCodingException e) {
            return null;
        }
    }

    private static int findTerminator(byte[] packet, int start) {
        for (int i = start; i < packet.length; i++) {
            if (packet[i] == 0) {
                return i;
            }
        }
        return -1;
    }

    private static boolean isDirectAccount(byte[] packet, int start, int end) {
        if (end - start > MAX_ACCOUNT_LENGTH) {
            return false;
        }
        for (int i = start; i < end; i++) {
            int value = packet[i] & 0xff;
            if (value < 0x21 || value > 0x7e) {
                return false;
            }
        }
        return true;
    }

    private static String decode(byte[] packet, int start, int end, Charset charset)
            throws CharacterCodingException {
        return charset.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT)
                .decode(ByteBuffer.wrap(packet, start, end - start)).toString();
    }

    private static boolean containsControl(String value) {
        for (int i = 0; i < value.length(); i++) {
            char character = value.charAt(i);
            if (Character.isISOControl(character)
                    || Character.getType(character) == Character.FORMAT) {
                return true;
            }
        }
        return false;
    }

    String getAccountName() {
        return accountName;
    }

    String getPassword() {
        return password;
    }
}
