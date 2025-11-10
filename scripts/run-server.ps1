$env:DB_URL = "jdbc:postgresql://localhost:5432/master_english"
$env:DB_USER = "master_dev"
$env:DB_PASSWORD = "master_dev_pwd"
./gradlew.bat :server:run