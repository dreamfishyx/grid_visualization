const ACCESS_TOKEN_KEY = 'access_token';
const REFRESH_TOKEN_KEY = 'refresh_token';
const EXPIRE_TIME_KEY = 'expire_time';
const USER_ID_KEY = 'user_id';
const USER_NAME_KEY = 'user_name';
const EMAIL_KEY = 'email';
const AVATAR_KEY = 'avatar';

export function getUserId() {
  return localStorage.getItem(USER_ID_KEY);
}

export function setUserId(userId) {
  localStorage.setItem(USER_ID_KEY, userId);
}

export function getUserName() {
  return localStorage.getItem(USER_NAME_KEY);
}

export function setUserName(userName) {
  localStorage.setItem(USER_NAME_KEY, userName);
}

export function getEmail() {
  return localStorage.getItem(EMAIL_KEY);
}

export function setEmail(email) {
  localStorage.setItem(EMAIL_KEY, email);
}

export function getAccessToken() {
  return localStorage.getItem(ACCESS_TOKEN_KEY);
}

export function setAccessToken(token) {
  localStorage.setItem(ACCESS_TOKEN_KEY, token);
}

export function getRefreshToken() {
  return localStorage.getItem(REFRESH_TOKEN_KEY);
}

export function setRefreshToken(token) {
  localStorage.setItem(REFRESH_TOKEN_KEY, token);
}

export function getAvatar() {
  return localStorage.getItem(AVATAR_KEY);
}

export function setAvatar(avatar) {
  localStorage.setItem(AVATAR_KEY, avatar);
}

export function removeAll() {
  localStorage.clear()
}

export function getExpireTime() {
  return localStorage.getItem(EXPIRE_TIME_KEY);
}

export function setExpireTime(time) {
  localStorage.setItem(EXPIRE_TIME_KEY, time);
}

