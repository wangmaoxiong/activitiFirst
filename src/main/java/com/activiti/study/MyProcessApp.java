package com.activiti.study;

import com.google.common.collect.Maps;
import org.activiti.engine.*;
import org.activiti.engine.form.FormProperty;
import org.activiti.engine.form.TaskFormData;
import org.activiti.engine.impl.form.DateFormType;
import org.activiti.engine.impl.form.StringFormType;
import org.activiti.engine.repository.Deployment;
import org.activiti.engine.repository.DeploymentBuilder;
import org.activiti.engine.repository.ProcessDefinition;
import org.activiti.engine.runtime.ProcessInstance;
import org.activiti.engine.task.Task;
import org.apache.commons.lang3.time.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.ParseException;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

@SuppressWarnings("all")
public class MyProcessApp {
    private static Logger LOGGER = LoggerFactory.getLogger(MyProcessApp.class);

    public static void main(String[] args) throws ParseException {
        LOGGER.info(" App start...");

        //1、创建流程引擎
        ProcessEngine processEngine = getProcessEngine();

        //2、部署流程定义文件
        ProcessDefinition processDefinition = getProcessDefinition(processEngine);

        //3、启动运行流程
        ProcessInstance processInstance = getProcessInstance(processEngine, processDefinition.getId());

        //4、处理流程任务
        handlingTasks(processEngine, processInstance);
        LOGGER.info(" App quit...");
    }

    /**
     * 处理流程任务
     *
     * @param processEngine
     * @param processInstance
     * @throws ParseException
     */
    private static void handlingTasks(ProcessEngine processEngine, ProcessInstance processInstance) throws ParseException {
        Scanner scanner = new Scanner(System.in);//从控制台手动输入数据
        while (processInstance != null && !processInstance.isEnded()) {
            TaskService taskService = processEngine.getTaskService();//获取任务服务
            List<Task> taskList = taskService.createTaskQuery().list();
            for (Task task : taskList) {
                LOGGER.info("当前待处理任务id [{}]，任务名称 [{}]", task.getId(), task.getName());
                Map<String, Object> dataMap = getStringObjectMap(processEngine, scanner, task);
                taskService.complete(task.getId(), dataMap);//提交任务/完成任务
                //重新获取流程实例
                processInstance = processEngine.getRuntimeService()
                        .createProcessInstanceQuery()
                        .processInstanceId(processInstance.getId())
                        .singleResult();
            }
        }
        scanner.close();
    }

    /**
     * 设置当前任务的表单参数。从控制台输入
     *
     * @param processEngine
     * @param scanner
     * @param task
     * @return
     * @throws ParseException
     */
    private static Map<String, Object> getStringObjectMap(ProcessEngine processEngine, Scanner scanner, Task task) throws ParseException {
        FormService formService = processEngine.getFormService();//表单服务
        TaskFormData taskFormData = formService.getTaskFormData(task.getId());//根据任务id获取任务表单数据对象
        List<FormProperty> formPropertyList = taskFormData.getFormProperties();//获取表单属性
        Map<String, Object> dataMap = Maps.newHashMap();
        for (FormProperty formProperty : formPropertyList) {
            String line = null;
            if (StringFormType.class.isInstance(formProperty.getType())) {
                LOGGER.info("请输入 {}", formProperty.getName());
                line = scanner.nextLine();
                dataMap.put(formProperty.getId(), line);
            } else if (DateFormType.class.isInstance(formProperty.getType())) {
                LOGGER.info("请输入 {}", formProperty.getName());
                line = scanner.nextLine();
                dataMap.put(formProperty.getId(), DateUtils.parseDate(line, "yyyy-MM-dd HH:mm:ss"));
            } else {
                LOGGER.warn("暂时不支持此类型 [{}]", formProperty.getType());
            }
            LOGGER.info("您成功输入 [{}]", line);
        }
        return dataMap;
    }

    /**
     * 启动运行流程
     *
     * @param processEngine
     * @param processDefinitionId
     */
    private static ProcessInstance getProcessInstance(ProcessEngine processEngine, String processDefinitionId) {
        RuntimeService runtimeService = processEngine.getRuntimeService();
        ProcessInstance processInstance = runtimeService.startProcessInstanceById(processDefinitionId);
        String processDefinitionKey_ = processInstance.getProcessDefinitionKey();
        LOGGER.info("启动流程，流程key [{}] ", processDefinitionKey_);//输出：启动流程，流程key [approvalFor2Level]
        return processInstance;
    }

    /**
     * 部署流程定义文件
     *
     * @param processEngine
     * @return
     */
    private static ProcessDefinition getProcessDefinition(ProcessEngine processEngine) {
        RepositoryService repositoryService = processEngine.getRepositoryService();//获取存储服务
        DeploymentBuilder deploymentBuilder = repositoryService.createDeployment();//创建部署构建器
        //通过类路径下的 .bpmn 文件进行构建。.bpmn 文件的本质其实就是一个定义好的 .xml 文件
        deploymentBuilder.addClasspathResource("myProcess1.bpmn");
        //部署流程定义文件。此时 Activiti 会自动将 .bpmn 中的数据添加到数据库中，以备后续数据库操作
        Deployment deployment = deploymentBuilder.deploy();
        String deploymentId = deployment.getId();
        String deploymentKey = deployment.getKey();
        String deploymentName = deployment.getName();
        Date deploymentTime = deployment.getDeploymentTime();

        //输出：部署id [1],部署key [null],部署名称 [null],部署时间 [Wed Jul 24 09:40:00 CST 2019]
        LOGGER.info("部署id [{}],部署key [{}],部署名称 [{}],部署时间 [{}]", deploymentId, deploymentKey, deploymentName, deploymentTime);

        ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery()
                .deploymentId(deploymentId)
                .singleResult();
        String processDefinitionName = processDefinition.getName();
        String processDefinitionId = processDefinition.getId();
        String processDefinitionKey = processDefinition.getKey();
        //输出：流程定义文件名 [二级审批流程]，流程id [approvalFor2Level:1:4],流程key [approvalFor2Level]
        LOGGER.info("流程定义文件名 [{}]，流程id [{}],流程key [{}]", processDefinitionName, processDefinitionId, processDefinitionKey);
        return processDefinition;
    }

    /**
     * 创建流程引擎
     *
     * @return
     */
    private static ProcessEngine getProcessEngine() {
        //StandaloneInMemProcessEngineConfiguration：标准的基于内存数据库的流程引擎配置.
        ProcessEngineConfiguration pecfg = ProcessEngineConfiguration.createStandaloneInMemProcessEngineConfiguration();
        //buildProcessEngine：通过流程引擎配置构建流程引擎，此时会自动创建 activiti 的 28 张表
        ProcessEngine processEngine = pecfg.buildProcessEngine();
        String name = processEngine.getName();
        String version = ProcessEngine.VERSION;
        LOGGER.info("流程引擎名称 [{}]，版本 [{}]", name, version);//输出：流程引擎名称 [default]，版本 [6.0.0.4]
        return processEngine;
    }
}
