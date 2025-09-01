# Guide

## Requirements

Make sure `java` is available in your system path.  
You can check with:  

```bash
java -version
```

## Run Demo

To run the demo, open **5 separate terminals** and execute the following commands (one per terminal, in order):

1. **Main server** - the Main Server is mandatory to allow other seeds to join the cluster.
  ```bash
  java -jar agario*.jar 127.0.0.1 19000 main
  ```

2. **Child server Nr. 1**
  ```bash
   java -jar agario*.jar 127.0.0.1 19001 child
  ```


3. **Child server Nr. 2**
  ```bash
   java -jar agario*.jar 127.0.0.1 19002 child
  ```


4. **Client Nr. 1**
  ```bash
   java -jar agario*.jar 127.0.0.1 19003 client
  ```

5. **Client Nr. 2**
  ```bash
   java -jar agario*.jar 127.0.0.1 19004 client
  ```
