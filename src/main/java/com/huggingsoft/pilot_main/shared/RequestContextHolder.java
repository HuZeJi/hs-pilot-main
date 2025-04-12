package com.huggingsoft.pilot_main.shared;

public final class RequestContextHolder {
    // Use InheritableThreadLocal if you need context propagation across threads
    // spawned by the request thread (use with caution, understand implications).
    // For standard request processing, ThreadLocal is usually sufficient.
    private static final ThreadLocal<RequestContext> CONTEXT_HOLDER = new ThreadLocal<>();

    private RequestContextHolder() {
        // Private constructor for utility class
    }

    public static void setContext(RequestContext context) {
        if (context == null) {
            clearContext(); // Avoid setting null, clear instead
        } else {
            CONTEXT_HOLDER.set(context);
        }
    }

    public static RequestContext getContext() {
        // Return null or throw exception if context not set, based on your requirements
        return CONTEXT_HOLDER.get();
    }

    // Convenience method
    public static String getRequestId() {
        RequestContext ctx = getContext();
        return (ctx != null) ? ctx.getRequestId() : null;
    }

    public static void clearContext() {
        CONTEXT_HOLDER.remove();
    }
}
