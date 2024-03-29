package com.arcadia.tahoe.argo;

import io.fabric8.kubernetes.api.model.Namespaced;
import io.fabric8.kubernetes.client.CustomResource;
import io.fabric8.kubernetes.model.annotation.Group;
import io.fabric8.kubernetes.model.annotation.Kind;
import io.fabric8.kubernetes.model.annotation.Version;

import java.util.Map;

@Version("v1alpha1")
@Group("argoproj.io")
@Kind("WorkflowTemplate")
public class ArgoWorkflowTemplate extends CustomResource<Map, Map> implements Namespaced {
}
