// Copyright 2017 Google Inc.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
//
////////////////////////////////////////////////////////////////////////////////

package com.google.crypto.tink.subtle;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.google.crypto.tink.HybridDecrypt;
import com.google.crypto.tink.HybridEncrypt;
import com.google.crypto.tink.TestUtil;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.concurrent.Callable;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

/**
 * Unit tests for {@link NaClCryptoBox}.
 */
@RunWith(Suite.class)
@SuiteClasses({
    NaClCryptoBoxTest.XSalsa20Poly1305Test.class,
    NaClCryptoBoxTest.ChaCha20Poly1305Test.class,
    NaClCryptoBoxTest.XChaCha20Poly1305Test.class
})
public class NaClCryptoBoxTest {

  /**
   * Base class for all NaClCryptoBox tests.
   */
  public abstract static class BaseTest {
    public abstract HybridEncrypt getHybridEncrypt(final byte[] peersPublicKey);
    public abstract HybridDecrypt getHybridDecrypt(final byte[] privateKey);

    /**
     * Each test vector is of the following format:
     * new String[] { privateKey, peersPublicKey, nonce, ciphertext, tag, expectedPlaintext}
     */
    public String[][] getTestVectors() {
      return new String[0][0];
    }

    public void testEncryptionDecryption(int count, Callable<byte[]> plaintextGenerator)
        throws Exception {
      for (int i = 0; i < count; i++) {
        byte[] peersPrivateKey = NaClCryptoBox.generatePrivateKey();
        byte[] peersPublicKey = NaClCryptoBox.getPublicKey(peersPrivateKey);
        HybridEncrypt hybridEncrypt = getHybridEncrypt(peersPublicKey);
        HybridDecrypt hybridDecrypt = getHybridDecrypt(peersPrivateKey);
        byte[] expectedInput = plaintextGenerator.call();
        byte[] ciphertext = hybridEncrypt.encrypt(expectedInput, null);
        byte[] actualInput = null;
        try {
          actualInput = hybridDecrypt.decrypt(ciphertext, null);
          assertTrue(Arrays.equals(expectedInput, actualInput));
        } catch (Throwable e) {
          String error = String.format(
              "\n\nMessage: %s\nPeersPrivateKey: %s\nPeersPublicKey: %s\nCiphertext: %s\n"
                  + "Decrypted Msg: %s\n",
              TestUtil.hexEncode(expectedInput),
              TestUtil.hexEncode(peersPrivateKey),
              TestUtil.hexEncode(peersPublicKey),
              TestUtil.hexEncode(ciphertext),
              actualInput == null ? "null" : TestUtil.hexEncode(actualInput));
          fail(error + e.getMessage());
        }
      }
    }

    @Test
    public void testVectors() throws GeneralSecurityException {
      for (String[] tv : getTestVectors()) {
        HybridDecrypt hybridDecrypt = getHybridDecrypt(
            TestUtil.hexDecode(tv[0]));
        byte[] plaintext = null;
        try {
          plaintext = hybridDecrypt.decrypt(
              TestUtil.hexDecode(tv[1] + tv[2] + tv[3] + tv[4]), null);
          assertEquals(tv[5], TestUtil.hexEncode(plaintext));
        } catch (Throwable e) {
          String error = String.format(
              "\n\nMessage: %s\nPrivateKey: %s\nPeersPublicKey: %s\nNonce: %s\nCiphertext: %s\n"
                  + "Tag: %s\nDecrypted Msg: %s\n",
              tv[5], tv[0], tv[1], tv[2], tv[3], tv[4],
              plaintext == null ? "null" : TestUtil.hexEncode(plaintext));
          fail(error + e.getMessage());
        }
      }
    }

    @Test
    public void testLessThanABlockEncryptionDecryption() throws Exception {
      // less than a block size.
      testEncryptionDecryption(100, new Callable<byte[]>() {
        @Override
        public byte[] call() throws Exception {
          return Random.randBytes(new java.util.Random().nextInt(64));
        }
      });
    }

    @Test
    public void testMoreThanABlockEncryptionDecryption() throws Exception {
      // more than a block size but less than two blocks.
      testEncryptionDecryption(100, new Callable<byte[]>() {
        @Override
        public byte[] call() throws Exception {
          return Random.randBytes(64 + new java.util.Random().nextInt(64));
        }
      });
    }

    @Test
    public void testRandomEncryptionDecryption() throws Exception {
      // [0, 300) bytes plaintext.
      testEncryptionDecryption(300, new Callable<byte[]>() {
        @Override
        public byte[] call() throws Exception {
          return Random.randBytes(new java.util.Random().nextInt(300));
        }
      });
    }

    @Test
    public void testEmptyEncryptionDecryption() throws Exception {
      testEncryptionDecryption(1, new Callable<byte[]>() {
        @Override
        public byte[] call() throws Exception {
          return new byte[0];
        }
      });
    }
  }

  /**
   * Tests for NaClCryptoBox with XSalsa20Poly1305.
   */
  public static class XSalsa20Poly1305Test extends BaseTest {
    @Override
    public HybridEncrypt getHybridEncrypt(byte[] peersPublicKey) {
      return NaClCryptoBox.hybridEncryptWithXSalsa20Poly1305(peersPublicKey);
    }

    @Override
    public HybridDecrypt getHybridDecrypt(byte[] privateKey) {
      return NaClCryptoBox.hybridDecryptWithXSalsa20Poly1305(privateKey);
    }

    @Override
    public String[][] getTestVectors() {
      return new String[][] {
          // Section 10, Example 1 in decrypt mode
          // http://cr.yp.to/highspeed/naclcrypto-20090310.pdf
          new String[] {
              "77076d0a7318a57d3c16c17251b26645df4c2f87ebc0992ab177fba51db92c2a",
              "de9edb7d7b7dc1b4d35b61c2ece435373f8343c85b78674dadfc7e146f882b4f",
              "69696ee955b62b73cd62bda875fc73d68219e0036b7a0b37",
              "8e993b9f48681273c29650ba32fc76ce48332ea7164d96a4476fb8c531a1186a"
                  + "c0dfc17c98dce87b4da7f011ec48c97271d2c20f9b928fe2270d6fb863d51738"
                  + "b48eeee314a7cc8ab932164548e526ae90224368517acfeabd6bb3732bc0e9da"
                  + "99832b61ca01b6de56244a9e88d5f9b37973f622a43d14a6599b1f654cb45a74"
                  + "e355a5",
              "f3ffc7703f9400e52a7dfb4b3d3305d9",
              "be075fc53c81f2d5cf141316ebeb0c7b5228c52a4c62cbd44b66849b64244ffc"
                  + "e5ecbaaf33bd751a1ac728d45e6c61296cdc3c01233561f41db66cce314adb31"
                  + "0e3be8250c46f06dceea3a7fa1348057e2f6556ad6b1318a024a838f21af1fde"
                  + "048977eb48f59ffd4924ca1c60902e52f0a089bc76897040e082f93776384864"
                  + "5e0705"
          },
          // Below test vectors are computed with NaCl library.
          // Empty message
          new String[] {
              "7e679fb76b339df00294a98b64b99067b07c47b4209fd944fbc2042ae80e96a0",
              "648c71fa31afef2c0f1a902b8fa9d3718fb14b8e4f54abee54f765dae3a09d07",
              "75a352307203f6a79c849e69ff7a9eab0f08fbbf74f9bc8a",
              "",
              "bdea28db2dfad0e25150ff8142d81c75",
              ""
          },
          // 100 byte message
          new String[] {
              "5c3dcfef409f7ff554594d381a8566fb3bcd6ca94f084f2157ccc008d597b63c",
              "efeff8e951aa8eb2c304e2e9699f92528c89216f037e89f268f3a9457555bb45",
              "6c7a8118fcb7048546f8147988d7176fbdfd5b4b0f05b86b",
              "e2ecf6806681746f25c0267231154a900ffbf4e8426a1122303c74c8e9b85331"
                  + "2784f8473b448cd824344a0f0786eff946c5ef22a3ec74641fb76cbd79fb8f0a"
                  + "0b4275693e920a25b0ea37384fa7ac8b3ad345fc779fc3f2dfa7643c66f5219c"
                  + "ac0bebda",
              "cd07d4198cd4642c678bb95bbfa28b11",
              "40c75e96381e9005b087dbe197a3318a3bced81ebca22ea372f6efc4db0dca23"
                  + "1505cfa89f1bde47e4d41b5bc9ef13799cc537ad46055c0aa49aad67182c1584"
                  + "54fac869b2ff6f717c2feb659cf498103faf48a99716c72643e7b099954cc2aa"
                  + "56e844fa"
          },
          // 300 byte message
          new String[] {
              "c8190cddb37cbe21d6a28d08e2935680d01f57088b9d72f1b1366879081e4f7c",
              "a3ecb739a0a4bfdbc29b862b1785f55053093b7e43e8c81897a0beb3c2ac4a0b",
              "b0d1f53386797daed89b58c250b03922bde9ee0e9f56ed23",
              "85ca8eb3a0b8989d7b168e34ec83c9eaf2609604e4e0d098b56fcc726f822133"
                  + "e6c6ab0ac8c7e752d39dadf479e704eee71fa085b28b94a20bac23bbb242626a"
                  + "fa944bcef36ec576cc475a76844bee5c0dcecfc90734e6230eb64d848a15033f"
                  + "7494a5d371432d6903b37424a873c95d4a822f13aad91b5ea387c8f927b1d6c1"
                  + "29abf0fa7de29387493c4741be2f17e3e238a08a7113524e960fa5521265a63a"
                  + "625dd8895e31e4bed9f29618b9c0cfb5064b20b19a6b050bca44f5a5ad42b18d"
                  + "5df288b1b2ff8a7ebe51b6193bdc3b342c17209bb42da82ba85d057b233981ac"
                  + "b6333c2544fc060cd91ef31c008cfbee735cc2a3eb0135364b86a23c1af048eb"
                  + "2f5fcc28a68186daeae01866d10fa6b0e8ef53d7f16cb94bd506ce3be376c4a5"
                  + "af7c3ef52a47aecbbd54c946",
              "7b9647a328dac50889f9b0aa1acdd095",
              "734cd1ee676e927ea8ffd26d077bdfc4dd56afbc29309426ef5a8afc5865146d"
                  + "0c887334652e325dd0c5e713a72560a69c909b8c845f6a2ed899f0e35ac5f723"
                  + "099786f6680cb07f77cbe02aab85f5a1383c2f98d4d4da2bb0726ad3f2f50266"
                  + "229faa98eb6f804252e63a586de0631a792aef579b2b35826a04a0537bff4611"
                  + "ec0ee493066ee21d790c32c984523864c1ea630575f1415fe1f0d66cac654918"
                  + "4ebd51a8941c7383fce8b7a3d6d9ce33a98c2ce754aeb559fc6974a347751cfa"
                  + "1bb46b51c63066f3827cd0b5f01eb16a285c7ac5f42e3d9abd038d1594fc5336"
                  + "2f15dd0332c6bd94b71b7ced652f0024cfbf6bc0692b8a0c9649edbd5c120329"
                  + "3aa81b2984ea884742e9f907c631277295883cee5344e3432e307e153ef6439c"
                  + "552290937ddb1894adb800b8"
          }
      };
    }
  }

  /**
   * Tests for NaClCryptoBox with ChaCha20Poly1305.
   */
  public static class ChaCha20Poly1305Test extends BaseTest {
    @Override
    public HybridEncrypt getHybridEncrypt(byte[] peersPublicKey) {
      return NaClCryptoBox.hybridEncryptWithChaCha20Poly1305(peersPublicKey);
    }

    @Override
    public HybridDecrypt getHybridDecrypt(byte[] privateKey) {
      return NaClCryptoBox.hybridDecryptWithChaCha20Poly1305(privateKey);
    }
  }

  /**
   * Tests for NaClCryptoBox with XChaCha20Poly1305.
   */
  public static class XChaCha20Poly1305Test extends BaseTest {
    @Override
    public HybridEncrypt getHybridEncrypt(byte[] peersPublicKey) {
      return NaClCryptoBox.hybridEncryptWithXChaCha20Poly1305(peersPublicKey);
    }

    @Override
    public HybridDecrypt getHybridDecrypt(byte[] privateKey) {
      return NaClCryptoBox.hybridDecryptWithXChaCha20Poly1305(privateKey);
    }

    @Override
    public String[][] getTestVectors() {
      return new String[][] {
          // Below test vectors are computed with libsodium's
          // crypto_box_curve25519xchacha20poly1305_easy
          // Empty message
          new String[] {
              "05951e7b46328d8ca48248ac7821fef9889d812fac67b717fb1f423176d463bf",
              "9dc172c83a8512d870093af307be0ff956b2783d2637c8bfd8c7e9db691a9f79",
              "bb4dfd0360239936fa4bb76665ae9b5a91c2ba077d180894",
              "",
              "00d3bd2d8dc76e41b1e012ca5d04ddf9",
              ""
          },
          // 100 byte message
          new String[] {
              "54159cc27b86ec985053f997b70f7cc9d1f6062b241b14db02ab376da4a09877",
              "323e28c8f3580c8944f659f89a53509a680eeef431e92ce6fa1ab81ce270672a",
              "65544c7c180b7f5b66344818e6cd985cb84c23d308a70f39",
              "331d8c15836bbc34a7e4f59187777dcc7a9c3b152582aad735c773b026ee805d"
                  + "54b111b8d8e41a4ecbde3072014495e8352e0492c2de6c9be3037388eae46d9b"
                  + "74d02c1e1e4ac2df27da190817cedac1d38859a0cc3bc777c0d3ed00727bfd86"
                  + "01ffec7a",
              "c9a6c4d0cb7cef10c7c78c0cdce36e9a",
              "d0fa06c1bd1565db3e89e899ef0fcf7d7dfca72326d8e923b9109a69ed83be09"
                  + "7684ce7eec94a70be854655e09480b655e0ca76772a379b3acdbcff408d77673"
                  + "fe0b39201851c680850706779ad89589a54d4572c7c1d11166c0754c8213fa61"
                  + "98b5017d"
          },
          // 300 byte message
          new String[] {
              "1df2a74fad386275019c7591686ac87734c8273de71e02ecc8f5572eb7a19be3",
              "ac8c0a9ed7751cb51b39b06e0cc9ca3ff6609e39db3f1d5e5390b4eb50dd7840",
              "15263fd02cd77dc5f4a4c1a9b301b9829e0c96cd30b93bf4",
              "82863a0c4bcd8522e5d7c3bcd0889885c821a7e02f9d50ef5613d3beef802b12"
                  + "129f80c66b871bdbfbccb88d7fe5b816fd54c291c1918092f8ebeac10f8fb1f2"
                  + "17b0c17a2e6df077b56d84ec758e280847a51c860fd55487d1465c2a24c8f8d1"
                  + "58739527b2fe7e636681dfbce7cef5d33e8b7fe72e9b40bc7a6de65b2eb2ca3b"
                  + "1318cf3b812f8adf4c32c9bd44bc57ee36e50b5421e29691638098ee15a47dd1"
                  + "345feb2c1eadec16143604102fdf6b03ec645feee45ad0b96bf81ea382af5ef5"
                  + "20a1869f866407cd08b101e6df5098dbb19f188e68f9fbcce4833494f26f2b8c"
                  + "f0b991e1357565ede485627dd60852f7244e81e5e68bea4ed300d8db25b02eba"
                  + "71a4753158f247ff1bc475fbdbb66c819c5326f941535cd33701d5c9ba8fe202"
                  + "cb7168098d4e494b6bb4fd46",
              "500179e818132a0d21befe73984804aa",
              "25fa1d68191db7f725fe4f2e794b99a3da1d77d5aa99169b858ba1fcfc1815e6"
                  + "f2ba43364b33d2004f7f20993f65931b4852a1223213a4fb4161b77a11f179dd"
                  + "4938734940598135f427ea9cdaba0d6f0aba8c2a070601c9ac3c135762598e7f"
                  + "e2c7d8a6e3c5109090044cd3c455e7ec3469f6a5a5a900f6b87bf23a42626317"
                  + "634f02173a432c489385ab43717c5abcd0be16f861b88b902151135a92d0721e"
                  + "0a6126a2a5ec0e8b80b078c81f9b752f7fef5e04cac5886859d4c90997beb4c3"
                  + "a0598ebb7637b03b2f8f486f9137fd8a8ab40a93dc1bed6f17ad31309a605a98"
                  + "38416f58f12270550a0ed443d89a44eee0055e6450e683f3214781e9ef8298c6"
                  + "9c3086b6db09be6aff0c117718394181e1c7a41c1f352d22facd22e558fb0e79"
                  + "40e78b820f3aaeff91af82fa"
          },
      };
    }
  }
}
