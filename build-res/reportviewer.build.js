({
  appDir: "./module-scripts",
  optimize: "none",
  baseUrl: ".",
  dir: "../bin/scriptOutput",
  //Put in a mapping so that 'requireLib' in the
  //modules section below will refer to the require.js
  //contents.
  paths: {
    requireLib: 'require'
  },

  //Indicates the namespace to use for require/requirejs/define.
  namespace: "pen",

  modules: [
    {
      name: "reportviewer-app",
      include: ["reportviewer/reportviewer-main-module"],
      create: true
    }
  ]
})
