const fs = require('fs');
const path = require('path');

module.exports = (context) => {
  console.log('Running copy-jars hook for @albgen/capacitor-zpl-plugin');

  // Path to the plugin's android/libs folder
  const pluginLibs = path.join(
    context.opts.projectRoot,
    'node_modules/@albgen/capacitor-zpl-plugin/android/libs'
  );

  // Path to the test project's android/app/src/main/libs folder
  const targetLibs = path.join(
    context.opts.projectRoot,
    'android/app/src/main/libs'
  );

  // Check if the plugin's libs folder exists
  if (fs.existsSync(pluginLibs)) {
    // Create the target libs folder if it doesn't exist
    if (!fs.existsSync(targetLibs)) {
      fs.mkdirSync(targetLibs, { recursive: true });
    }

    // Copy all JAR files to the target location
    fs.readdirSync(pluginLibs).forEach(file => {
      if (file.endsWith('.jar')) {
        const src = path.join(pluginLibs, file);
        const dest = path.join(targetLibs, file);
        fs.copyFileSync(src, dest);
        console.log(`Copied ${file} to ${targetLibs}`);
      }
    });
  } else {
    console.log('Plugin libs folder not found:', pluginLibs);
  }
};