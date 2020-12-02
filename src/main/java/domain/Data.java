package domain;

public class Data {
  private final String method;
  private final String data;

  public Data(String method, String data) {
    this.method = method;
    this.data = data;
  }

  public String getMethod() {
    return method;
  }

  public String getData() {
    return data;
  }

}
