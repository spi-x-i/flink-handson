## Flink-JPMML-FF
This demo job has been runned along the related talk presented at last **Flink Forward 2017**, running in Berlin.
You can find the *flink-jpmml* presentation [here](https://www.youtube.com/watch?v=0rWvMZ6JSD8).

## Demo

`TextDemo` is the example job you should target to; it takes exactly one parameter:
- **--outputPath** your directory where you desire the main output which will be saved as text file.

You can easily run `TextDemo` in your IntelliJ environment either on a Flink Cluster.

You can find more information about the job throughout the code lines.

In order to run the demo you need to open a socket. You should
- open a terminal
- open a socket by `nc -lk 9999`

then through this socket you can send PMML model paths. You can find the demo employed model in the *resources* directory.

Perhaps you want also to know accuracies about these models:
- _svm_model_1.pmml_ 0.48
- _svm_model_2.pmml_ 0.72
- _svm_model_3.pmml_ 0.95

___
If you find any problem plesae, don't hesitate to catch me at
- andrea.spina@radicalbit.io
- spixi13@gmail.com
