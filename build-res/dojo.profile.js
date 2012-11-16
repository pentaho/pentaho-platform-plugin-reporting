dependencies = {
  layers:[
    // includes our custom Dojo artifacts
    {
      //place the file under dojoRootDir
      name:"dojo-reportviewer.js",
      layerDependencies:[
      ],
      dependencies:[
        "dojo.i18n",
        "dojo.parser",
        "dijit.Dialog",
        "dijit.Toolbar",
        "pentaho.common.Calendar",
        "pentaho.common.DateTextBox",
        "pentaho.common.Messages",
        "pentaho.common.MessageBox",
        "pentaho.common.Menu",
        "pentaho.common.MenuItem",
        "pentaho.common.PageControl",
        "pentaho.common.GlassPane",
        "pentaho.reportviewer.ReportDialog"
      ]
    }
  ],

  prefixes:[
    ["dijit", "../dijit"],
    ["dojox", "../dojox"],
    ["pentaho", "../pentaho"]
  ]

}