# IRSystem

### Team 05

Increase memory space in Maven project.

Follow these steps to adjust the memory settings for Maven in Eclipse:

1. Open Run Configurations: Right-click on your project in Eclipse's Project Explorer, and then select "Run As" > "Maven build..."
2. Create or Edit Configuration: In the "Edit Configuration" dialog, you can either create a new configuration or edit an existing one.
3. Configuration Tab: In the "Main" tab of the configuration dialog, ensure that the "Base directory" points to your project's root directory (where the 'pom.xml' is located).
4. Goals: In the "Goals" field, enter the Maven command you want to run, like 'clean', 'install', 'compile', etc. Make sure to use valid goals appropriate for your project.
5. JRE Tab: Switch to the "JRE" tab in the configuration dialog.
6. VM arguments: In the "VM arguments" field, specify the additional memory options using the '-Xmx' flag, like '-Xmx2g' to allocate 2 GB of memory. This will allocate 2 GB of maximum heap memory to the JVM.
7. Apply and Run: Click "Apply" to save the changes and then click "Run" to execute the Maven build with the updated memory settings.
 