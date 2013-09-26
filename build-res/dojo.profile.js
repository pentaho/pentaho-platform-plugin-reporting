/*!
* The Pentaho proprietary code is licensed under the terms and conditions
* of the software license agreement entered into between the entity licensing
* such code and Pentaho Corporation.
*
* This software costs money - it is not free
*
* Copyright 2002 - 2013 Pentaho Corporation.  All rights reserved.
*/

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
