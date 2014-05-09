<xsl:transform version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
<xsl:param name="product.slug"/>
<xsl:param name="version.label"/>
<xsl:template match="/">
<installation version="1.0">

    <!-- 
        The info section.
        The meaning of the tags should be natural ...
    -->
    <info>
      <appname><xsl:value-of select="$product.slug"/></appname>
      <appversion><xsl:value-of select="$version.label"/></appversion>
      <appsubpath><xsl:value-of select="$product.slug"/>-<xsl:value-of select="$version.label"/></appsubpath>
      <authors>
         <author name="R. Alexander Milowski" email="alex@milowski.com"/>
      </authors>
      <url>https://github.com/alexmilowski/xproclet</url>
      <javaversion>1.7</javaversion>
    </info>
    
    <!-- 
        The gui preferences indication.
        Sets the installer window to 640x480. It will not be able to change the size.
    -->
    <guiprefs width="640" height="480" resizable="no"/>
    
    <!-- 
        The locale section.
        Asks here to include the English and French langpacks.
    -->
    <locale>
      <langpack iso3="eng"/>
      <langpack iso3="fra"/>
      <langpack iso3="deu"/>
      <langpack iso3="spa"/>
      <langpack iso3="ita"/>
      <langpack iso3="jpn"/>

    </locale>
    
    <!-- 
        The resources section.
        The ids must be these ones if you want to use the LicencePanel and/or the InfoPanel.
    -->
    <resources>
        <res id="LicencePanel.licence" src="../../LICENSE"/>
    </resources>
    
    <!-- 
        The panels section.
        We indicate here which panels we want to use. The order will be respected.
    -->
    <panels>
        <panel classname="HelloPanel"/>
        <panel classname="LicencePanel"/>
        <panel classname="TargetPanel"/> 
        <panel classname="InstallPanel"/>
        <panel classname="FinishPanel"/>
    </panels>
    
    <!-- 
        The packs section.
        We specify here our packs.
    -->
    <packs>
        <pack name="Base" required="yes">
            <description>The base files</description>
            <fileset dir="{$product.slug}-{$version.label}" targetdir="$INSTALL_PATH"/>
            <parsable type="shell" targetfile="$INSTALL_PATH/server"/>
            <parsable type="shell" targetfile="$INSTALL_PATH/server.bat"/>
            <executable targetfile="$INSTALL_PATH/server" stage="never"/>
        </pack>
    </packs>
    
</installation>
</xsl:template>
</xsl:transform>

