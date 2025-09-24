package com.example;

import com.azure.core.credential.TokenCredential;
import com.azure.core.http.policy.HttpLogDetailLevel;
import com.azure.core.management.AzureEnvironment;
import com.azure.core.util.Context;
import com.azure.identity.DefaultAzureCredentialBuilder;
import com.azure.resourcemanager.AzureResourceManager;
import com.azure.resourcemanager.computefleet.ComputeFleetManager;
import com.azure.resourcemanager.computefleet.models.BaseVirtualMachineProfile;
import com.azure.resourcemanager.computefleet.models.CapacityType;
import com.azure.resourcemanager.computefleet.models.ComputeProfile;
import com.azure.resourcemanager.computefleet.models.Fleet;
import com.azure.resourcemanager.computefleet.models.FleetMode;
import com.azure.resourcemanager.computefleet.models.FleetProperties;
import com.azure.resourcemanager.computefleet.models.ImageReference;
import com.azure.resourcemanager.computefleet.models.LinuxConfiguration;
import com.azure.resourcemanager.computefleet.models.NetworkApiVersion;
import com.azure.resourcemanager.computefleet.models.OperatingSystemTypes;
import com.azure.resourcemanager.computefleet.models.RegularPriorityProfile;
import com.azure.resourcemanager.computefleet.models.SpotPriorityProfile;
import com.azure.resourcemanager.computefleet.models.CachingTypes;
import com.azure.resourcemanager.computefleet.models.DiskDeleteOptionTypes;
import com.azure.resourcemanager.computefleet.models.StorageAccountTypes;
import com.azure.resourcemanager.computefleet.models.VirtualMachine;
import com.azure.resourcemanager.computefleet.models.VirtualMachineScaleSetIPConfigurationProperties;
import com.azure.resourcemanager.computefleet.models.VirtualMachineScaleSetManagedDiskParameters;
import com.azure.resourcemanager.computefleet.models.VirtualMachineScaleSetIPConfiguration;
import com.azure.resourcemanager.computefleet.models.ApiEntityReference;
import com.azure.resourcemanager.computefleet.models.VirtualMachineScaleSetNetworkProfile;
import com.azure.resourcemanager.computefleet.models.VirtualMachineScaleSetOSDisk;
import com.azure.resourcemanager.computefleet.models.VirtualMachineScaleSetOSProfile;
import com.azure.resourcemanager.computefleet.models.VirtualMachineScaleSetStorageProfile;
import com.azure.resourcemanager.computefleet.models.DiskCreateOptionTypes;
import com.azure.resourcemanager.computeschedule.ComputeScheduleManager;
import com.azure.resourcemanager.computeschedule.models.DeleteResourceOperationResponse;
import com.azure.resourcemanager.computeschedule.models.ExecuteDeleteRequest;
import com.azure.resourcemanager.computeschedule.models.ExecutionParameters;
import com.azure.resourcemanager.computeschedule.models.GetOperationStatusRequest;
import com.azure.resourcemanager.computeschedule.models.ResourceOperation;
import com.azure.resourcemanager.computeschedule.models.ResourceOperationDetails;
import com.azure.resourcemanager.computeschedule.models.Resources;
import com.azure.resourcemanager.computefleet.models.VirtualMachineScaleSetNetworkConfiguration;
import com.azure.resourcemanager.computefleet.models.VirtualMachineScaleSetNetworkConfigurationProperties;
import com.azure.resourcemanager.computefleet.models.VmSizeProfile;
import com.azure.core.management.Region;
import com.azure.core.management.profile.AzureProfile;
import com.azure.core.http.rest.Response;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import com.azure.resourcemanager.computeschedule.models.RetryPolicy;
import com.azure.resourcemanager.computeschedule.models.GetOperationStatusResponse;
import com.azure.resourcemanager.computeschedule.models.OperationState;

public final class Main {
        final static String subscriptionId = "31352ba3-4576-4d93-9214-7c2d18b24067";
        final static String tenantID = "31352ba3-4576-4d93-9214-7c2d18b24067";
        final static AzureProfile profile = new AzureProfile(tenantID, subscriptionId, AzureEnvironment.AZURE);
        final static TokenCredential credential = new DefaultAzureCredentialBuilder()
                        .authorityHost(profile.getEnvironment().getActiveDirectoryEndpoint())
                        .build();
        private static final Region region = Region.UK_SOUTH;
        private static final String resourceGroupName = "DEMO-JAVA-SDK-RG";
        private static final String virtualNetworkName = "DEMO-VN";
        private static AzureResourceManager azureResourceManager;
        private static ComputeFleetManager computeFleetManager;
        private static ComputeScheduleManager computeScheduleManager;

        public static void main(String[] args) {
                // Set up the environment, create the clients, the resource group and virtual network
                setup();

                // Create 1K VMs of Regular priority using BulkActions
                createVMsUsingBulkActions(/* bulkActionsName */ "BA-1K-VMs",
                                /* capacityType */ CapacityType.VM,
                                /* spotCapacity */ 0,
                                /* regularCapacity */ 1000,
                                /* vmSizesProfile */ Arrays.asList(
                                                new VmSizeProfile().withName("Standard_F1s"),
                                                new VmSizeProfile().withName("Standard_DS1_v2"),
                                                new VmSizeProfile().withName("Standard_D2ads_v5"),
                                                new VmSizeProfile().withName("Standard_D8as_v5")));

                // Create 1K VCPUs of Regular priority using BulkActions
                createVMsUsingBulkActions(/* bulkActionsName */ "BA-1K-VCPUs",
                                /* capacityType */ CapacityType.VCPU,
                                /* spotCapacity */ 0,
                                /* regularCapacity */ 1000,
                                /* vmSizesProfile */ Arrays.asList(
                                                new VmSizeProfile().withName("Standard_F2s"),
                                                new VmSizeProfile().withName("Standard_DS2_v2"),
                                                new VmSizeProfile().withName("Standard_E2s_v3"),
                                                new VmSizeProfile().withName("Standard_D2as_v4")));

                // Register the subscription with the Microsoft.ComputeSchedule resource provider
                // This is required in order to delete VMs using the ScheduledActions BulkDelete feature
                azureResourceManager.providers().register("Microsoft.ComputeSchedule");

                // Get the list of VMs in the first BulkActions
                List<String> ba1kVMsList = listVMsInBulkAction("BA-1K-VMs");

                // Delete all VMs created by the first BulkActions using the ScheduledActions BulkDelete feature with forceDelete
                bulkDeleteByIds(ba1kVMsList, /* forceDelete */ true);

                // Get the list of VMs in the second BulkActions
                List<String> ba1kVcpusList = listVMsInBulkAction("BA-1K-VCPUs");

                // Delete half of the VMs created by the second BulkActions using the ScheduledActions BulkDelete feature with forceDelete
                bulkDeleteByIds(ba1kVcpusList.subList(0, ba1kVcpusList.size() / 2), /* forceDelete */ true);

                // Get the list of all remaining VMs in the resource group (half of the second BulkActions)
                List<String> remainingVMsList = listVMsInRG();

                // Delete all remaining VMs in the resource group using the ScheduledActions BulkDelete feature with forceDelete
                bulkDeleteByIds(remainingVMsList, /* forceDelete */ true);

                // Delete RG to cleanup all BulkActions
                deleteResourceGroup();
        }

        private static void setup() {
                azureResourceManager = AzureResourceManager.configure()
                                .withLogLevel(HttpLogDetailLevel.BASIC)
                                .authenticate(credential, profile).withDefaultSubscription();

                computeFleetManager = ComputeFleetManager.authenticate(credential, profile);
                computeScheduleManager = ComputeScheduleManager.authenticate(credential, profile);

                azureResourceManager.networks().define(virtualNetworkName).withRegion(region)
                                .withNewResourceGroup(resourceGroupName)
                                .withAddressSpace("10.0.0.0/16").defineSubnet("default")
                                .withAddressPrefix("10.0.0.0/18").attach().create();

                azureResourceManager.resourceGroups().define(resourceGroupName).withRegion(region)
                                .create();
        }

        private static <T> List<List<T>> batch(List<T> source, int batchSize) {
                List<List<T>> batches = new java.util.ArrayList<>();
                for (int i = 0; i < source.size(); i += batchSize) {
                        batches.add(source.subList(i, Math.min(i + batchSize, source.size())));
                }
                return batches;
        }

        private static GetOperationStatusRequest getOpsRequest(
                        Response<DeleteResourceOperationResponse> deleteResponse) {
                List<String> operationIds = deleteResponse.getValue().innerModel().results()
                                .stream().map(ResourceOperation::operation)
                                .map(ResourceOperationDetails::operationId)
                                .collect(Collectors.toList());

                return new GetOperationStatusRequest().withOperationIds(operationIds)
                                .withCorrelationid(UUID.randomUUID().toString());
        }

        private static ExecuteDeleteRequest getDeleteRequest(List<String> resourceIds,
                        boolean forceDelete) {
                return new ExecuteDeleteRequest()
                                .withResources(new Resources().withIds(resourceIds))
                                .withForceDeletion(forceDelete)
                                .withExecutionParameters(new ExecutionParameters()
                                                .withRetryPolicy(new RetryPolicy().withRetryWindowInMinutes(15)))
                                .withCorrelationid(UUID.randomUUID().toString());
        }

        private static boolean isPollingComplete(
                        Response<GetOperationStatusResponse> getOpsResponse) {
                List<OperationState> operationStates = getOpsResponse.getValue().innerModel()
                                .results().stream().map(ResourceOperation::operation)
                                .map(ResourceOperationDetails::state).collect(Collectors.toList());

                return operationStates.stream()
                                .allMatch(state -> state == OperationState.SUCCEEDED);
        }

        private static void bulkDeleteByIds(List<String> resourceIds, boolean forceDelete){
                batch(resourceIds, 100).parallelStream().forEach(batch -> {
                        // Start the deletion
                        Response<DeleteResourceOperationResponse> deleteResponse =
                                        computeScheduleManager.scheduledActions()
                                                        .virtualMachinesExecuteDeleteWithResponse(
                                                                        region.name(),
                                                                        getDeleteRequest(batch, forceDelete),
                                                                        Context.NONE);

                        // Poll until completion
                        boolean pollingCompleted = false;
                        while (!pollingCompleted) {
                                Response<GetOperationStatusResponse> getOpsResponse =
                                                computeScheduleManager.scheduledActions()
                                                                .virtualMachinesGetOperationStatusWithResponse(
                                                                                region.name(),
                                                                                getOpsRequest(deleteResponse),
                                                                                Context.NONE);

                                pollingCompleted = isPollingComplete(getOpsResponse);
                        }
                });
        }

        private static Fleet createVMsUsingBulkActions(String bulkActionsName,
                        CapacityType capacityType, int spotCapacity, int regularCapacity,
                        List<VmSizeProfile> vmSizesProfile) {

                FleetProperties props = new FleetProperties().withMode(FleetMode.INSTANCE)
                                .withCapacityType(capacityType)
                                .withSpotPriorityProfile(new SpotPriorityProfile().withCapacity(spotCapacity))
                                .withRegularPriorityProfile(new RegularPriorityProfile().withCapacity(regularCapacity))
                                .withVmSizesProfile(vmSizesProfile)
                                .withComputeProfile(new ComputeProfile()
                                                .withComputeApiVersion("2023-07-01")
                                                .withBaseVirtualMachineProfile(
                                                                new BaseVirtualMachineProfile()
                                                                                .withStorageProfile(
                                                                                                new VirtualMachineScaleSetStorageProfile()
                                                                                                                .withImageReference(
                                                                                                                                new ImageReference()
                                                                                                                                                .withPublisher("Canonical")
                                                                                                                                                .withOffer("UbuntuServer")
                                                                                                                                                .withSku("18.04-LTS")
                                                                                                                                                .withVersion("latest"))
                                                                                                                .withOsDisk(new VirtualMachineScaleSetOSDisk()
                                                                                                                                .withOsType(OperatingSystemTypes.LINUX)
                                                                                                                                .withCreateOption(
                                                                                                                                                DiskCreateOptionTypes.FROM_IMAGE)
                                                                                                                                .withDeleteOption(
                                                                                                                                                DiskDeleteOptionTypes.DELETE)
                                                                                                                                .withCaching(CachingTypes.READ_WRITE)
                                                                                                                                .withManagedDisk(
                                                                                                                                                new VirtualMachineScaleSetManagedDiskParameters()
                                                                                                                                                                .withStorageAccountType(
                                                                                                                                                                                StorageAccountTypes.STANDARD_LRS))))
                                                                                .withOsProfile(new VirtualMachineScaleSetOSProfile()
                                                                                                .withComputerNamePrefix(
                                                                                                                "sample-compute")
                                                                                                .withAdminUsername(
                                                                                                                "sample-user")
                                                                                                .withAdminPassword(
                                                                                                                "***********") // use a valid password here
                                                                                                .withLinuxConfiguration(
                                                                                                                new LinuxConfiguration()
                                                                                                                                .withDisablePasswordAuthentication(
                                                                                                                                                false)))
                                                                                .withNetworkProfile(
                                                                                                new VirtualMachineScaleSetNetworkProfile()
                                                                                                                .withNetworkApiVersion(
                                                                                                                                NetworkApiVersion.V2020_11_01)
                                                                                                                .withNetworkInterfaceConfigurations(
                                                                                                                                java.util.Arrays.asList(
                                                                                                                                                new VirtualMachineScaleSetNetworkConfiguration()
                                                                                                                                                                .withName("nicConfig1")
                                                                                                                                                                .withProperties(new VirtualMachineScaleSetNetworkConfigurationProperties()
                                                                                                                                                                                .withPrimary(true)
                                                                                                                                                                                .withIpConfigurations(
                                                                                                                                                                                                java.util.Arrays.asList(
                                                                                                                                                                                                                new VirtualMachineScaleSetIPConfiguration()
                                                                                                                                                                                                                                .withName("ipConfig1")
                                                                                                                                                                                                                                .withProperties(new VirtualMachineScaleSetIPConfigurationProperties()
                                                                                                                                                                                                                                                .withPrimary(true)
                                                                                                                                                                                                                                                .withSubnet(new ApiEntityReference()
                                                                                                                                                                                                                                                                .withId("/subscriptions/"
                                                                                                                                                                                                                                                                                + subscriptionId
                                                                                                                                                                                                                                                                                + "/resourceGroups/"
                                                                                                                                                                                                                                                                                + resourceGroupName
                                                                                                                                                                                                                                                                                + "/providers/Microsoft.Network/virtualNetworks/"
                                                                                                                                                                                                                                                                                + virtualNetworkName
                                                                                                                                                                                                                                                                                + "/subnets/default")))))))))));

                return computeFleetManager.fleets().define(bulkActionsName).withRegion(region)
                                .withExistingResourceGroup(resourceGroupName).withProperties(props)
                                .create();
        }

        private static List<String> listVMsInBulkAction(String bulkActionsName) {
                return computeFleetManager.fleets()
                                .listVirtualMachines(resourceGroupName, bulkActionsName).stream()
                                .map(VirtualMachine::id).collect(Collectors.toList());
        }

        private static List<String> listVMsInRG() {
                return azureResourceManager.virtualMachines().listByResourceGroup(resourceGroupName)
                                .stream()
                                .map(com.azure.resourcemanager.compute.models.VirtualMachine::id)
                                .collect(Collectors.toList());
        }

        private static void deleteResourceGroup() {
                azureResourceManager.resourceGroups().beginDeleteByName(resourceGroupName);
        }
}
