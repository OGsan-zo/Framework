package other;

public class Mapping {

    private String methodName;
    private String className;

    public Mapping() {
    }

    public Mapping(String methodName, String className) {
        this.methodName = methodName;
        this.className = className;
    }

    public String getMethodName() {
        return methodName;
    }

    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    @Override
    public String toString() {
        return "Mapping{" +
                "methodName='" + methodName + '\'' +
                ", className='" + className + '\'' +
                '}';
    }
}
