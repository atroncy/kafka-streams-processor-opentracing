# kafka-streams-processor-opentracing


Start a new span for any Processor #process(), re-using ProcessorContext HeaderValue


Argument fileName for .properties (optional)  

groupId = allow to filter which class implementing Processor will start a new span. If you don't want to start new span on every forEach/peek/filter/map/mapValues etc... this might be usefull. (optional)  
headerFilter = regex pattern to filter which header key to use. (optional)  


Note:
- this is not prod ready and is not published to any repo, feel free fork it to your need !  
- you should configure your globalTracer, don't trace everything !  
- this create a new span every time process is called, it does not attach to a parrentSpan from headerContext, since parentSpan are not allowed with kafka for good reason. See propagated span context ChildOf versus FollowsFrom [opentracing docs best practices](https://opentracing.io/docs/best-practices/).

ProcessorContext expose header since kafka-streams: 2.0.0