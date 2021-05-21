# Table transfer between Databases

# Purpose

**DB 사용자에게 DB 간 Table 복사에 작은 도움을 주기위해 만들었다.**

Oracle에서 MariaDB로 DB 전환을 아래와 같은 방법으로 시도했다.

![image](https://user-images.githubusercontent.com/22446581/117782631-c4a88780-b27c-11eb-8ed6-f83229889fe0.png)

1. Oracle에서 CSV 혹은 Insert SQL을 dump로 만든다.
2. MariaDB로 테이블을 비슷하게 생성한다.
3. MaraiDB의 load data infile 을 실행.

반복적인 프로세스에도 에러가 생기기 마련이다. 

1. 문자에 Enter 가 있으면 CSV 파일의 형식이 망가진다.
2. DB 날짜의 Date String Format이 달라, 전처리 작업이 필요하다.
3. Blob과 Clob 컬럼이 존재하면, 별도의 저장공간과 Update 문장이 필요하다.

하지만 가장 큰 문제는...**에러가 나면 처음부터 다시 시작해야 한다.**

이런 반복+수작업을 해결하기 위해, Java로 JDBC Batch를 사용해 Command Line Application을 만들어 보았다.

## Contraints

이 프로그램은 아래 2가지를 인정하고 시작했다.

* Database 사이에 완벽한 Datatype match는 존재하지 않는다.
  * 데이터에 손상없이 이동하기 위해 노력했다.
* 많은 데이터를 최대한 빠르게 옮기기 위해, 테이블의 제약조건은 제외한다.
  * 제약조건이 걸려있으면 Insert 로직이 느려지는 현상이 발생한다.
  * 제약조건은 Insert가 끝난 후, DB가 지원하는 Client로 DDL을 실행하는 편이 빠르다.

# Database

현재까지 테스트한 DB 는 아래와 같다.

1. Oracle 11gR2
2. MariaDB-1:10.5.9+maria~focal

# Column Type

## Oracle to MariaDB

| Oracle                             | ColumnType | MariaDB  |
| ---------------------------------- | ---------- | -------- |
| VARCHAR2 (1  <= length < 2000)     | STRING     | VACHAR   |
| VARCHAR2  (2000 <= length <= 4000) | STRING     | TEXT     |
| NUMBER(p,s)                        | NUMERIC    | DECIMAL  |
| DATE                               | DATETIME   | DATETIME |
| CLOB                               | CLOB       | LONGTEXT |
| BLOB                               | BLOB       | BLOB     |

## MariaDB to Oracle

| MariaDB  | ColumnType | Oracle      |
| -------- | ---------- | ----------- |
| VACHAR   | STRING     | VARCHAR2    |
| TEXT     | STRING     | VARCHAR2    |
| DECIMAL  | NUMERIC    | NUMBER(p,s) |
| INT      | NUMERIC    | NUMBER(p)   |
| BIGINT   | NUMERIC    | NUMBER(p)   |
| DATETIME | DATETIME   | DATE        |
| LONGTEXT | CLOB       | CLOB        |
| BLOB     | BLOB       | BLOB        |

# 실행방법

 JDK와 JVM은 아래 숫자로 실행하는 것을 추천한다.

* JDK : Openjdk 11
* JVM
  * Clob, Blob 컬럼 미포함 
    * 최소 메모리 : 120 mb
    * 최소 메모리 : 300 mb
  * Clob, Blob 컬럼 포함 
    * 최소 메모리 : 512 mb
    * 최대 메모리 : 1024 mb



## Executable Jar 실행.

Executable Jar로 Build 후 실행할 수 있다. 

```powershell
PS C:\jdk11> bin\java.exe -jar .\빌드파일.jar
Picked up JAVA_TOOL_OPTIONS: -Djava.net.preferIPv4Stack=true
Choose Source Database Vendor Type (ORACLE, MARIADB) : ORACLE
url : localhost:1521:XE
id : sys
password : 123456
10:45:18.358 [main] INFO  o.c.o.d.c.DatabaseConnection - ORACLE Connection Created (URL : localhost:1521:XE)
Choose target Database Vendor Type (ORACLE, MARIADB) : MARIADB
url : 127.0.0.1:3306/mydb
id : root
password : 123456
10:45:42.730 [main] INFO  o.c.o.d.c.DatabaseConnection - MARIADB Connection Created (URL : 127.0.0.1:3306/mydb)
Table Name : 테이블이름
```



## IDE Junit Test 실행.(Recommend)

Junit 단위 테스트로 실행할 수 있다. 실행에 검증까지 한번에 쓸 수 있어 추천하는 사용법이다.

```java
public class Oracle2MySqlTableMigrationTest {
    private static final ConnectionFactory factory = ConnectionFactory.getFactory();

    @Test
    @DisplayName("20000만건 테이블 이름을 주어진다. | 테이블 Migration 시작할 때 | 정상 종료한다.")
    public void test5() throws SQLException {
        setSourceOracle();
        setTargetMariaDB();

        ApplicationService applicationService = new ApplicationService();
        applicationService.beginApplication("테이블 이름");
    }

    private void setTargetMariaDB() {
        DBVendor vendor = DBVendor.MARIADB;
        String url = "127.0.0.1:3306/mydb";
        String id = "root";
        String password = "123456";

        factory.setTargetConnection(new ConnectionInfo(url, id, password, vendor));
    }

    private void setSourceOracle() {
        DBVendor vendor = DBVendor.ORACLE;
        String url = "127.0.0.1:1521:xe";
        String id = "sys";
        String password = "123456";

        factory.setSourceConnection(new ConnectionInfo(url, id, password, vendor));
    }
}

```



# 성능테스트

Oracle 11gR2(6 core)와 MariaDB-1:10.5.9+maria~focal(4 core)로 테스트 해본 결과다.

## Oracle to MariaDB

|                        | Row Count | Column Count | Memory(mb) |     Cost |
| ---------------------: | --------: | -----------: | ---------: | -------: |
|       문자, 숫자, 날짜 |     20000 |           43 |      13 mb |  18 sec. |
|       문자, 숫자, 날짜 |  13000000 |          244 |     6.5 gb | 111 min. |
| 문자, 숫자, 날짜, Clob |    340000 |           51 |    16.6 gb |  92 min. |
| 문자, 숫자, 날짜, Blob |     20000 |           11 |     204 mb |  90 sec. |

## MariaDB to Oracle

|                        | Row Count | Column Count | Memory(mb) |    Cost |
| ---------------------: | --------: | -----------: | ---------: | ------: |
|       문자, 숫자, 날짜 |     20917 |           43 |      17 mb |  8 sec. |
|       문자, 숫자, 날짜 |  13000000 |          244 |     6.3 gb | 31 min. |
| 문자, 숫자, 날짜, Clob |    340000 |           51 |      22 gb |  4 min. |
| 문자, 숫자, 날짜, Blob |     20000 |           11 |     222 mb |  1 min. |

# Trouble shooting

## 작은 max_allowed_packet 설정으로 MariaDB Blob 테이블 Insert 누락 

기본적인 Loop는 1000 건에 1번씩 Insert 를 시도 하지만, Insert 문장이 너무 길면 mysql 에서 사용못하는 경우가 있다. 저버의 max_allowed_packet을 늘리는 작업을 수행한다.

