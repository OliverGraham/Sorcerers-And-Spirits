/**
 * Sorcerers & Spirits — Apple account-deletion token revocation (SS-296).
 *
 * Apple Guideline 5.1.1(v) requires that an app offering Sign in with Apple revokes the user's
 * tokens when their account is deleted. The client cannot do this (Firebase never exposes the Apple
 * refresh token, and the client secret must stay off-device), so the app hands this endpoint a fresh
 * one-time authorizationCode obtained by re-authenticating with Apple at delete time. This script:
 *   1. builds the Apple client secret (an ES256 JWT signed with the .p8 key),
 *   2. exchanges the authorizationCode at /auth/token for a refresh token,
 *   3. revokes that token at /auth/revoke.
 *
 * Requires the jsrsasign library pasted into a second script file (see README). Set the five Script
 * Properties listed in the README before deploying.
 */

var APPLE_TOKEN_URL = 'https://appleid.apple.com/auth/token';
var APPLE_REVOKE_URL = 'https://appleid.apple.com/auth/revoke';
var APPLE_AUDIENCE = 'https://appleid.apple.com';
var CLIENT_SECRET_LIFETIME_SECONDS = 300;

function doPost(e) {
  try {
    var body = JSON.parse(e.postData.contents);
    var props = PropertiesService.getScriptProperties();

    if (body.token !== props.getProperty('SHARED_TOKEN')) {
      return jsonResult(false);
    }

    var authorizationCode = body.authorizationCode;
    if (!authorizationCode) {
      return jsonResult(false);
    }

    var clientSecret = buildClientSecret(props);
    var refreshToken = exchangeCodeForRefreshToken(props, clientSecret, authorizationCode);
    if (!refreshToken) {
      return jsonResult(false);
    }

    var didRevoke = revokeToken(props, clientSecret, refreshToken);
    return jsonResult(didRevoke);
  } catch (error) {
    return jsonResult(false);
  }
}

function buildClientSecret(props) {
  var now = Math.floor(Date.now() / 1000);
  var header = { alg: 'ES256', kid: props.getProperty('APPLE_KEY_ID') };
  var payload = {
    iss: props.getProperty('APPLE_TEAM_ID'),
    iat: now,
    exp: now + CLIENT_SECRET_LIFETIME_SECONDS,
    aud: APPLE_AUDIENCE,
    sub: props.getProperty('APPLE_BUNDLE_ID'),
  };
  var privateKeyPem = props.getProperty('APPLE_PRIVATE_KEY');
  return KJUR.jws.JWS.sign('ES256', JSON.stringify(header), JSON.stringify(payload), privateKeyPem);
}

function exchangeCodeForRefreshToken(props, clientSecret, authorizationCode) {
  var response = UrlFetchApp.fetch(APPLE_TOKEN_URL, {
    method: 'post',
    contentType: 'application/x-www-form-urlencoded',
    muteHttpExceptions: true,
    payload: {
      client_id: props.getProperty('APPLE_BUNDLE_ID'),
      client_secret: clientSecret,
      grant_type: 'authorization_code',
      code: authorizationCode,
    },
  });

  if (response.getResponseCode() !== 200) {
    return null;
  }
  return JSON.parse(response.getContentText()).refresh_token || null;
}

function revokeToken(props, clientSecret, refreshToken) {
  var response = UrlFetchApp.fetch(APPLE_REVOKE_URL, {
    method: 'post',
    contentType: 'application/x-www-form-urlencoded',
    muteHttpExceptions: true,
    payload: {
      client_id: props.getProperty('APPLE_BUNDLE_ID'),
      client_secret: clientSecret,
      token: refreshToken,
      token_type_hint: 'refresh_token',
    },
  });
  return response.getResponseCode() === 200;
}

function jsonResult(success) {
  return ContentService
    .createTextOutput(JSON.stringify({ success: success }))
    .setMimeType(ContentService.MimeType.JSON);
}
