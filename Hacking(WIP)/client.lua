local checkWeaponThread, curAtmLocationIndex, curHacking = false, nil, false
local add, percent, curTaken, rewardTotal, justHacked, hackerRep = nil, nil, nil, nil, false, nil
local curTicketLocationIndex = nil
local curShockingEvent = nil
local trafficLight, curLightType = nil, nil

Citizen.CreateThread(function()
    while TMC == nil do
        Citizen.Wait(1000)
    end

    if TMC.IsDev then
        while not TMC.Functions.IsPlayerLoaded() do
            Citizen.Wait(50)
        end
        Citizen.Wait(200)
    end

    for k, v in pairs(Config.AtmHacks.Locations) do
        local atmCoords = v
        TMC.Functions.AddCircleZone('atmHackLocation', atmCoords, 1.0, {
            useZ = true,
            data = {locationIndex = k}
        })
    end

    for k, v in pairs(Config.TrafficLightHacks_a.Locations) do
        local trafficLightHackCoords = v

        TMC.Functions.AddCircleZone('trafficLightHackLocation',  vector3(trafficLightHackCoords.x, trafficLightHackCoords.y, trafficLightHackCoords.z + 1.0), 1.0, {
            useZ = true,
            data = {
                locationIndex = k,
                trafficLightHash = 'prop_traffic_01a',
                lightType = 'a'
            }
            
        })
    end

    for k, v in pairs(Config.TrafficLightHacks_b.Locations) do
        local trafficLightHackCoords = v

        TMC.Functions.AddCircleZone('trafficLightHackLocation', vector3(trafficLightHackCoords.x, trafficLightHackCoords.y, trafficLightHackCoords.z + 1.0), 1.0, {
            useZ = true,
            data = {
                locationIndex = k,
                trafficLightHash = 'prop_traffic_01b',
                lightType = 'b'
            }
            
        })
    end

    for k, v in pairs(Config.TrafficLightHacks_d.Locations) do
        local trafficLightHackCoords = v

        TMC.Functions.AddCircleZone('trafficLightHackLocation', vector3(trafficLightHackCoords.x, trafficLightHackCoords.y, trafficLightHackCoords.z + 1.0), 1.0, {
            useZ = true,
            data = {
                locationIndex = k,
                trafficLightHash = 'prop_traffic_01d',
                lightType = 'd'
            }
            
        })
    end

    atmHackPrompt = TMC.Functions.CreatePromptGroup({{
        Id = 'atm_hack',
        Complete = function()
            if not justHacked then
                if not curHacking then
                    hackATM(curAtmLocationIndex)
                else
                    TMC.Functions.SimpleNotify('You are already stealing from this ATM', 'error', 6000)
                end
            else
                TMC.Functions.SimpleNotify('You already tried hacking this ATM', 'error', 6000)
            end
        end,
        Title = 'Hack Into ATM Machine',
        AutoComplete = true,
        Description = 'Attempt to hack into the machine.',
        Icon = 'fa-duotone fa-code'
    }})

    trafficLightHackPrompt = TMC.Functions.CreatePromptGroup({{
        Id = 'traffic_light_hack',
        Complete = function()
            if not justHacked then
                if not curHacking then
                    hackTrafficLight(curTrafficLightLocationIndex)
                else
                    TMC.Functions.SimpleNotify('You are already hacking this traffic light', 'error', 6000)
                end
            else
                TMC.Functions.SimpleNotify('You already tried hacking this traffic light', 'error', 6000)
            end
        end,
        Title = 'Hack Traffic Light',
        AutoComplete = false,
        Description = 'Attempt to hack into the light pole.',
        Icon = 'fa-duotone fa-code'
    }})

    TMC.Functions.AddPolyZoneEnterHandler('atmHackLocation', function(data)
        curAtmLocationIndex = data.locationIndex
        if TMC.Functions.HasItem('hacking_terminal') and not TMC.Functions.IsPolice(true) then
            TMC.Functions.ShowPromptGroup(atmHackPrompt)
        end
    end)

    TMC.Functions.AddPolyZoneExitHandler('atmHackLocation', function(data)
        curAtmLocationIndex = nil
        justHacked = false
        TMC.Functions.HidePromptGroup(atmHackPrompt)
    end)

    TMC.Functions.AddPolyZoneEnterHandler('trafficLightHackLocation', function(data)
        curTrafficLightLocationIndex = data.locationIndex
        curLightType = data.lightType
        trafficLight = TMC.Functions.GetClosestObject({data.trafficLightHash})
        if TMC.Functions.HasItem('hacking_terminal') and not TMC.Functions.IsPolice(true) then
            TMC.Functions.ShowPromptGroup(trafficLightHackPrompt)
        end
    end)

    TMC.Functions.AddPolyZoneExitHandler('trafficLightHackLocation', function(data)
        curTrafficLightLocationIndex = nil
        justHacked = false
        TMC.Functions.HidePromptGroup(trafficLightHackPrompt)
    end)

end)

function hackATM(curAtmLocationIndex)
    hackerRep = LocalPlayer.state.rep.hacker
    local hackDifficulty = 0
    local hackTimer = 0
    if curHacking then return; end

    TMC.Functions.TriggerServerCallback('crime:hacking:isAtmHacked', function(result)
        if result then
            TMC.Functions.SimpleNotify('This machine appears to have already been hacked', 'error', 6000)
            curHacking = false
            return
        end
        --if TMC.Common.OnlinePlayersWithJob('police', true, Config.MinCrimeGrade) < Config.MinPoliceatmHack then
        --    TMC.Functions.SimpleNotify("You can't do this at the moment", "error")
        --    TMC.Functions.HidePromptGroup(atmHackPrompt)
         --   return
        --end
        
        if TMC.Functions.HasItem('hacking_terminal') and TMC.Functions.HasItem('trojan_usb') then
            curHacking = true
            TriggerEvent('dispatch:client:atmHack')
            TMC.Functions.TriggerServerEvent("tmc:log", "atmHack", "ATM Hack", "yellow", string.format("**%s %s (%s)** has started hacking the ATM at **%s**", LocalPlayer.state.charinfo.firstname, LocalPlayer.state.charinfo.lastname, LocalPlayer.state.citizenid, curAtmLocationIndex))

            local targetAtmLocation = Config.AtmHacks.Locations[curAtmLocationIndex]
            local seq = OpenSequenceTask()
            TaskTurnPedToFaceCoord(0, targetAtmLocation.xyz, 1250)
            CloseSequenceTask(seq)
            TaskPerformSequence(PlayerPedId(), seq)
            local sequenceStartTime = GetGameTimer()
            Citizen.Wait(0) -- Sequence will not be reported for first frame after starting
            repeat Citizen.Wait(0) until GetSequenceProgress(PlayerPedId()) == -1 -- -1 = Sequence is not running (Is done, or was exited somehow)
            ClearSequenceTask(seq)

            TaskStartScenarioInPlace(PlayerPedId(), "WORLD_HUMAN_STAND_MOBILE", 0, true)
            if hackerRep >= 100  then
                hackDifficulty  = math.random(1, 2)
                hackTimer = math.random(14000, 17000)
            elseif hackerRep >= 50 then
                hackDifficulty = math.random(2, 4)
                hackTimer = math.random(12000, 14000)
            else
                hackDifficulty = math.random(3, 4)
                hackTimer = math.random(11000, 13000)
            end
            TriggerEvent('tmc_minigames:start', 'Hackconnect', { difficulty = hackDifficulty, attempts = 3, timer = hackTimer, background = 1 }, function(data)
                ClearPedTasks(playerPedId)
                FreezeEntityPosition(playerPedId, false)
                curHacking = false
                local amountGiven = math.random(200, 600)
                if TMC.Common.RandomChance(1, 100, 10) then
                    TMC.Functions.TriggerServerEvent('TMC:Server:RemoveItem', 'trojan_usb', 1)
                    TMC.Functions.SimpleNotify('You successfully hacked the ATM but broke your usb', 'error')
                end
                TMC.Functions.TriggerServerEvent("crime:hacking:atmHackReward", amountGiven)
                TriggerEvent("police:client:createFingerPrint")
                TMC.Functions.TriggerServerEvent('crime:hacking:lockHackingAtm', curAtmLocationIndex)
                -- Trigger a shocking event to make peds react to the crime
                -- 102 just makes them be a bit concerned and then jog (not run) away
                curShockingEvent = AddShockingEventAtPosition(102, targetAtmLocation.xyz, ((GetGameTimer() - sequenceStartTime) + 50000.0) / 1000.0)
            end, function(data)
                ClearPedTasks(playerPedId)
                FreezeEntityPosition(playerPedId, false)
                curHacking = false
                TriggerEvent("police:client:createFingerPrint")
                if TMC.Common.RandomChance(1, 100, 30) then
                    TMC.Functions.TriggerServerEvent('TMC:Server:RemoveItem', 'trojan_usb', 1)
                    return TMC.Functions.SimpleNotify('You failed to hack the ATM and broke your usb ', 'error', 5000)
                else
                    return TMC.Functions.SimpleNotify('You failed to hack the ATM', 'error', 5000)
                end
            end)
        else
            return TMC.Functions.SimpleNotify('You do not have the tools to do this', 'error', 10000)
        end

        if not manualClear then
            ClearPedTasks(playerPedId)
        end
    end, curAtmLocationIndex)
end

function hackTrafficLight(curTrafficLightLocationIndex)
    local hackDifficulty = 0
    local hackTimer = 0
    hackerRep = LocalPlayer.state.rep.hacker

    if curHacking then return; end
    curHacking = true

    TMC.Functions.TriggerServerCallback('crime:hacking:isTrafficLightLocked', function(result)
        if result then
            TMC.Functions.SimpleNotify('This light seems to have already been hacked recently', 'error', 6000)
            curHacking = false
            return
        end
        if TMC.Functions.HasItem('hacking_terminal') and TMC.Functions.HasItem('trojan_usb') then
            TriggerEvent('dispatch:client:trafficLightHack')
            curHacking = true
            local targetTrafficLightLocation
            if curLightType == 'a' then
                targetTrafficLightLocation = Config.TrafficLightHacks_a.Locations[curTrafficLightLocationIndex]
            elseif curLightType == 'b' then
                targetTrafficLightLocation = Config.TrafficLightHacks_b.Locations[curTrafficLightLocationIndex]
            else
                targetTrafficLightLocation = Config.TrafficLightHacks_d.Locations[curTrafficLightLocationIndex]
            end
            local seq = OpenSequenceTask()
            TaskTurnPedToFaceCoord(0, targetTrafficLightLocation.xyz, 1250)
            CloseSequenceTask(seq)
            TaskPerformSequence(PlayerPedId(), seq)
            local sequenceStartTime = GetGameTimer()
            Citizen.Wait(0) -- Sequence will not be reported for first frame after starting
            repeat Citizen.Wait(0) until GetSequenceProgress(PlayerPedId()) == -1 -- -1 = Sequence is not running (Is done, or was exited somehow)
            ClearSequenceTask(seq)
            TriggerEvent('dispatch:client:trafficLightHack')
            TaskStartScenarioInPlace(PlayerPedId(), "WORLD_HUMAN_STAND_MOBILE", 0, true)
            if hackerRep >= 100  then
                hackDifficulty  = math.random(1, 2)
                hackTimer = math.random(11000, 13000)
            elseif hackerRep >= 50 then
                hackDifficulty = math.random(2, 4)
                hackTimer = math.random(9000, 11000)
            else
                hackDifficulty = math.random(3, 4)
                hackTimer = math.random(8000, 10000)
            end
            TriggerEvent('tmc_minigames:start', 'Bruteforce', {difficulty = hackDifficulty, stages = 3, timer = hackTimer, background = 1}, function(data)
                ClearPedTasksImmediately(playerPedId)
                FreezeEntityPosition(playerPedId, false)
                if TMC.Common.RandomChance(1, 100, 5) then
                    TMC.Functions.TriggerServerEvent('TMC:Server:RemoveItem', 'trojan_usb', 1)
                    TMC.Functions.SimpleNotify('You hacked the light but broke your usb', 'error')
                end
                TMC.Functions.SimpleNotify('You succeeded in hacking the light', 'success', 5000)
                TriggerTrafficLightSpaz(targetTrafficLightLocation)
                TMC.Functions.TriggerServerEvent('crime:hacking:lockTrafficLight', curTrafficLightLocationIndex)
                -- Trigger a shocking event to make peds react to the crime
                -- 102 just makes them be a bit concerned and then jog (not run) away
                curShockingEvent = AddShockingEventAtPosition(102, targetTrafficLightLocation.xyz, ((GetGameTimer() - sequenceStartTime) + 50000.0) / 1000.0)
            end, function()
                curHacking = false
                ClearPedTasksImmediately(playerPedId)
                FreezeEntityPosition(playerPedId, false)
                if TMC.Common.RandomChance(1, 100, 20) then
                    TMC.Functions.TriggerServerEvent('TMC:Server:RemoveItem', 'trojan_usb', 1)
                    return TMC.Functions.SimpleNotify('You failed to hack the light and broke your usb', 'error', 5000)
                else
                    return TMC.Functions.SimpleNotify('You failed to hack the light', 'error', 5000)
                end
            end)
        else
            curHacking = false
            return TMC.Functions.SimpleNotify('You do not have the tools to do this', 'error', 10000)
        end
        TMC.Functions.TriggerServerEvent("tmc:log", "trafficLightHack", "Traffic Light Hack", "yellow", string.format("**%s %s (%s)** has started hacking the traffic light at **%s**", LocalPlayer.state.charinfo.firstname, LocalPlayer.state.charinfo.lastname, LocalPlayer.state.citizenid, curTrafficLightLocationIndex))
    end, curTrafficLightLocationIndex)
end

function TriggerTrafficLightSpaz(targetTrafficLightLocation)
    print('in triggertrafficlightspaz')
    justHacked = true
    curHacking = false
    TMC.Functions.StartParticleFxInArea(100, vector3(targetTrafficLightLocation.x, targetTrafficLightLocation.y, targetTrafficLightLocation.z + 2.7), "spark", "trafficlight_spark", vector3(80.0, GetEntityHeading(trafficLight), 0.0))
    TMC.Functions.TriggerServerEvent('crime:hacking:trafficlightreward', trafficLight)
    print(targetTrafficLightLocation)
    TMC.Functions.TriggerServerEvent('crime:hacking:server:changeTrafficLights', trafficLight, targetTrafficLightLocation)
    SetTimeout(15000, function()
        print("should stop particle")
        TMC.Functions.TriggerServerEvent("particle:server:StopParticle", "trafficlight_spark")
    end)
end

RegisterNetEvent('crime:hacking:changeTrafficLights')
AddEventHandler('crime:hacking:changeTrafficLights', function(light, coords)
    -- There is no need to change it for players that are too far
    if #(GetEntityCoords(PlayerPedId()) - coords) > 150 then return end

    local count = 0
	while (count <= 15) do
        SetEntityTrafficlightOverride(light, 0)
	    Wait(150)
	    SetEntityTrafficlightOverride(light, 1)
	    Wait(150)
	    SetEntityTrafficlightOverride(light, 2)
	    Wait(150)
        count = count + 1
    end

	SetEntityTrafficlightOverride(light, 0)
	Wait(1000 * 60 * 10) --How long should it be green?
	SetEntityTrafficlightOverride(light, 3)
end)

RegisterNetEvent('TMC:Client:OnPlayerUnload', function()
    local checkWeaponThread, curAtmLocationIndex, curHacking = false, nil, false
    local add, percent, curTaken, rewardTotal = nil, nil, nil, nil
    local curTicketLocationIndex = nil
    TMC.Functions.RemoveAnimDict('grab')
    TMC.Functions.RemoveAnimDict('car_down_attack')
end)
