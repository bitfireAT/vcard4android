
# keep all vCard properties/parameters (used via reflection)
-keep class ezvcard.io.scribe.** { *; }
-keep class ezvcard.property.** { *; }
-keep class ezvcard.parameter.** { *; }

# AGP seems to remove this class, but ezvcard.io uses it. See https://github.com/bitfireAT/davx5/issues/499
-keep class javax.xml.namespace.QName { *; }
