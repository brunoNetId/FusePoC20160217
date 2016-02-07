<?xml version="1.0" encoding="UTF-8" ?>

<xsl:stylesheet version="2.0" 
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform" 
	xmlns:xs="http://www.w3.org/2001/XMLSchema"
	xmlns:fn="http://www.w3.org/2005/xpath-functions"
	xmlns:xdt="http://www.w3.org/2005/xpath-datatypes"
	xmlns:err="http://www.w3.org/2005/xqt-errors"
	exclude-result-prefixes="xs xdt err fn">

	<xsl:output method="xml" indent="yes"/>
	
	<xsl:template match="/">
		
<PublicList>
			<xsl:apply-templates/>
		
</PublicList>
	</xsl:template>
	
	<!-- we filter out non users, as we consider them private --> 
	<xsl:template match="//customer[@type!='user']"/>
	
	
	<xsl:template match="//customer[@type='user']">
		
<customer>
			
<name><xsl:value-of select="./name"/></name>
			
<email><xsl:value-of select="./email"/></email>
		
</customer>
	</xsl:template>

</xsl:stylesheet>
