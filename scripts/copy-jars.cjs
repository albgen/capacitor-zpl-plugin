const fs = require('fs');
const path = require('path');
const chalk = require('chalk');

const logPrefix = chalk.blue('[Copy-Jars]');

const projectRoot = process.env.INIT_CWD || process.cwd();
console.log(`${logPrefix} Running copy-jars script for @albgen/capacitor-zpl-plugin`);
console.log(`${logPrefix} Project root: ${projectRoot}`);

const pluginLibs = path.join(projectRoot, 'node_modules/@albgen/capacitor-zpl-plugin/android/libs');
const targetLibs = path.join(projectRoot, 'android/app/src/main/libs');

console.log(`${logPrefix} Plugin libs path: ${pluginLibs}`);
console.log(`${logPrefix} Target libs path: ${targetLibs}`);

if (fs.existsSync(pluginLibs)) {
  console.log(`${logPrefix} Plugin libs folder exists. Contents:`, fs.readdirSync(pluginLibs));
  if (!fs.existsSync(targetLibs)) {
    console.log(`${logPrefix} Creating target libs folder: ${targetLibs}`);
    fs.mkdirSync(targetLibs, { recursive: true });
  }

  fs.readdirSync(pluginLibs).forEach(file => {
    if (file.endsWith('.jar')) {
      const src = path.join(pluginLibs, file);
      const dest = path.join(targetLibs, file);
      console.log(`${logPrefix} Copying ${file} from ${src} to ${dest}`);
      fs.copyFileSync(src, dest);
      console.log(`${logPrefix} Copied ${file} to ${targetLibs}`);
    }
  });
} else {
  console.log(`${logPrefix} Plugin libs folder not found: ${pluginLibs}`);
}