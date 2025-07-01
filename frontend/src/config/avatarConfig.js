// 头像配置文件
export const avatarList = [
  '/src/assets/avatars/avatar1.png',
  '/src/assets/avatars/avatar2.png',
  '/src/assets/avatars/avatar3.png',
  '/src/assets/avatars/avatar4.png',
  '/src/assets/avatars/avatar5.png',
  '/src/assets/avatars/avatar6.png',
  '/src/assets/avatars/avatar7.png',
];

// 获取随机头像
export const getRandomAvatar = () => {
  const randomIndex = Math.floor(Math.random() * avatarList.length);
  return avatarList[randomIndex];
}; 