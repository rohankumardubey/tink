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

package com.google.crypto.tink;

import static com.google.crypto.tink.TestUtil.assertExceptionContains;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.google.crypto.tink.TinkProto.KeyStatusType;
import com.google.crypto.tink.TinkProto.Keyset;
import com.google.crypto.tink.TinkProto.KeysetInfo;
import com.google.crypto.tink.TinkProto.OutputPrefixType;
import java.security.GeneralSecurityException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * Tests for Util.
 */
@RunWith(JUnit4.class)
public class UtilTest {
  @Test
  public void testValidateKeyset() throws Exception {
    String keyValue = "01234567890123456";
    Keyset keyset =  TestUtil.createKeyset(TestUtil.createKey(
        TestUtil.createHmacKeyData(keyValue.getBytes("UTF-8"), 16),
        -42,
        KeyStatusType.ENABLED,
        OutputPrefixType.TINK));
    try {
      Util.validateKeyset(keyset);
    } catch (GeneralSecurityException e) {
      fail("Valid keyset; should not throw Exception: " + e);
    }

    // Empty keyset.
    try {
      Util.validateKeyset(Keyset.newBuilder().build());
      fail("Invalid keyset. Expect GeneralSecurityException");
    } catch (GeneralSecurityException e) {
      assertExceptionContains(e, "empty keyset");
    }

    // Primary key is disabled.
    Keyset invalidKeyset = TestUtil.createKeyset(TestUtil.createKey(
        TestUtil.createHmacKeyData(keyValue.getBytes("UTF-8"), 16),
        42,
        KeyStatusType.DISABLED,
        OutputPrefixType.TINK));
    try {
      Util.validateKeyset(invalidKeyset);
      fail("Invalid keyset. Expect GeneralSecurityException");
    } catch (GeneralSecurityException e) {
      assertExceptionContains(e, "keyset doesn't contain a valid primary key");
    }

    // Multiple primary keys.
    invalidKeyset = TestUtil.createKeyset(
        TestUtil.createKey(
            TestUtil.createHmacKeyData(keyValue.getBytes("UTF-8"), 16),
            42,
            KeyStatusType.ENABLED,
            OutputPrefixType.TINK),
        TestUtil.createKey(
            TestUtil.createHmacKeyData(keyValue.getBytes("UTF-8"), 16),
            42,
            KeyStatusType.ENABLED,
            OutputPrefixType.TINK)
    );
    try {
      Util.validateKeyset(invalidKeyset);
      fail("Invalid keyset. Expect GeneralSecurityException");
    } catch (GeneralSecurityException e) {
      assertExceptionContains(e, "keyset contains multiple primary keys");
    }
  }

  /**
   * Tests that getKeysetInfo doesn't contain key material.
   */
  @Test
  public void testGetKeysetInfo() throws Exception {
    String keyValue = "01234567890123456";
    Keyset keyset =  TestUtil.createKeyset(TestUtil.createKey(
        TestUtil.createHmacKeyData(keyValue.getBytes("UTF-8"), 16),
        42,
        KeyStatusType.ENABLED,
        OutputPrefixType.TINK));
    assertTrue(keyset.toString().contains(keyValue));

    KeysetInfo keysetInfo = Util.getKeysetInfo(keyset);
    assertFalse(keysetInfo.toString().contains(keyValue));
  }

  @Test
  public void testAssertExceptionContains() throws Exception {
    assertExceptionContains(new GeneralSecurityException("abc"), "abc");

    try {
      assertExceptionContains(new GeneralSecurityException("abc"), "def");
    } catch (AssertionError e) {
      assertExceptionContains(
        e, "Got exception with message \"abc\", expected it to contain \"def\".");
    }
  }

  // TODO(thaidn): add tests for other functions.
}
