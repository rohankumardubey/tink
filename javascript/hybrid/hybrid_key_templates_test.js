/**
 * @license
 * Copyright 2020 Google LLC
 * SPDX-License-Identifier: Apache-2.0
 */

goog.module('tink.hybrid.HybridKeyTemplatesTest');
goog.setTestOnly('tink.hybrid.HybridKeyTemplatesTest');

const {AeadKeyTemplates} = goog.require('google3.third_party.tink.javascript.aead.aead_key_templates');
const EciesAeadHkdfPrivateKeyManager = goog.require('tink.hybrid.EciesAeadHkdfPrivateKeyManager');
const HybridKeyTemplates = goog.require('tink.hybrid.HybridKeyTemplates');
const {PbEciesAeadHkdfKeyFormat, PbEllipticCurveType, PbHashType, PbOutputPrefixType, PbPointFormat} = goog.require('google3.third_party.tink.javascript.internal.proto');

describe('hybrid key templates test', function() {
  it('ecies p256 hkdf hmac sha256 aes128 gcm', function() {
    // Expects function to create a key with following parameters.
    const expectedCurve = PbEllipticCurveType.NIST_P256;
    const expectedHkdfHashFunction = PbHashType.SHA256;
    const expectedAeadTemplate = AeadKeyTemplates.aes128Gcm();
    const expectedPointFormat = PbPointFormat.UNCOMPRESSED;
    const expectedOutputPrefix = PbOutputPrefixType.TINK;

    // Expected type URL is the one supported by EciesAeadHkdfPrivateKeyManager.
    const manager = new EciesAeadHkdfPrivateKeyManager();
    const expectedTypeUrl = manager.getKeyType();

    const keyTemplate = HybridKeyTemplates.eciesP256HkdfHmacSha256Aes128Gcm();

    expect(keyTemplate.getTypeUrl()).toBe(expectedTypeUrl);
    expect(keyTemplate.getOutputPrefixType()).toBe(expectedOutputPrefix);

    // Test values in key format.
    const keyFormat =
        PbEciesAeadHkdfKeyFormat.deserializeBinary(keyTemplate.getValue());
    const params = keyFormat.getParams();
    expect(params.getEcPointFormat()).toBe(expectedPointFormat);

    // Test KEM params.
    const kemParams = params.getKemParams();
    expect(kemParams.getCurveType()).toBe(expectedCurve);
    expect(kemParams.getHkdfHashType()).toBe(expectedHkdfHashFunction);

    // Test DEM params.
    const demParams = params.getDemParams();
    expect(demParams.getAeadDem()).toEqual(expectedAeadTemplate);

    // Test that the template works with EciesAeadHkdfPrivateKeyManager.
    manager.getKeyFactory().newKey(keyTemplate.getValue_asU8());
  });

  it('ecies p256 hkdf hmac sha256 aes128 ctr hmac sha256', function() {
    // Expects function to create a key with following parameters.
    const expectedCurve = PbEllipticCurveType.NIST_P256;
    const expectedHkdfHashFunction = PbHashType.SHA256;
    const expectedAeadTemplate = AeadKeyTemplates.aes128CtrHmacSha256();
    const expectedPointFormat = PbPointFormat.UNCOMPRESSED;
    const expectedOutputPrefix = PbOutputPrefixType.TINK;

    // Expected type URL is the one supported by EciesAeadHkdfPrivateKeyManager.
    const manager = new EciesAeadHkdfPrivateKeyManager();
    const expectedTypeUrl = manager.getKeyType();

    const keyTemplate =
        HybridKeyTemplates.eciesP256HkdfHmacSha256Aes128CtrHmacSha256();

    expect(keyTemplate.getTypeUrl()).toBe(expectedTypeUrl);
    expect(keyTemplate.getOutputPrefixType()).toBe(expectedOutputPrefix);

    // Test values in key format.
    const keyFormat =
        PbEciesAeadHkdfKeyFormat.deserializeBinary(keyTemplate.getValue());
    const params = keyFormat.getParams();
    expect(params.getEcPointFormat()).toBe(expectedPointFormat);

    // Test KEM params.
    const kemParams = params.getKemParams();
    expect(kemParams.getCurveType()).toBe(expectedCurve);
    expect(kemParams.getHkdfHashType()).toBe(expectedHkdfHashFunction);

    // Test DEM params.
    const demParams = params.getDemParams();
    expect(demParams.getAeadDem()).toEqual(expectedAeadTemplate);

    // Test that the template works with EciesAeadHkdfPrivateKeyManager.
    manager.getKeyFactory().newKey(keyTemplate.getValue_asU8());
  });
});