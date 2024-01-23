local harvestrep, productionrep = nil, nil
local curBlip, inField, hasStartedHarvest, fieldSelected, finishCollection = nil, nil, false, nil, false
local StartPosRandom, bindropoff = nil, nil
local HasDropOff, dropOffArea, dropOffBin, dropOffSuccess, fieldAccessGranted, inQueue, DropOffBinObject, inHZCount = nil, nil, nil, nil, false, false, nil, 0
local cokelab, cokelabgrade = nil, nil
local isHarvesting, isProcessing = false, false
local dropOffBinZone, dropOffPZ = nil, nil
local cokeFieldPedInteract, cokeDropOffPedPrompt, cokeStartPedPrompt, cokeFieldPromptGroup, cokeLabEntryExitlab1, harvestFieldPromptGroup, abandonedTime, abandonedThread = nil, nil, nil, nil, nil, nil, nil, false
local cokeLabSoakPrompt, cokeLabPrepPrompt, cokeLabCombinePrompt, cokeLabSortPrompt, cokeLabBagPrompt = nil, nil, nil, nil, nil
local blipTable, barrelTable, curIngredients, generateLabFunction = {}, {}, {}, {}
local labInfo = {}
local fieldAbandoned, fieldAssigned = false, false
local curBarrel, barrelToUse, curBarrelCoords, curIndex  = false, false, false, false

local soakingLeaves = {}
local labZones = {}
local curMissionGiver = false


local zoneCount = 0

local cokefield = {
	[1] = false,
	[2] = false,
	[3] = false,
	[4] = false,
}

local CokeFieldData = {
	[1] = {},
	[2] = {},
	[3] = {},
	[4] = {},
}

local curHarvest = false
local curCokeYard = false
local currMissionGiver = false
local curCokeYardData = nil
cokeFieldsCreated = false

CokeMarkerColour = {
	["harvested"] = {
		r = 235,
		g = 23,
		b = 23,
	},
	["pending"] = {
		r = 235,
		g = 126,
		b = 23,
	},
	["processing"] = {
		r = 23,
		g = 69,
		b = 235,
	},
}

Citizen.CreateThread(function()
    while TMC == nil do
        Citizen.Wait(1000)
    end


    while not TMC.Functions.IsPlayerLoaded() do
        Citizen.Wait(50)
    end
    Citizen.Wait(400)
    if LocalPlayer.state.rep['cokeharvest'] > 2250 then
        harvestrep = 2250
    else
        harvestrep = LocalPlayer.state.rep['cokeharvest']
    end
    productionrep = LocalPlayer.state.rep['cokeproduction']
    Citizen.Wait(500)

    -- Cocaine starts
    for k,v in pairs(Config.Cocaine.StartPos) do
        TMC.Functions.CreateInteractionPed('cokestartped'..k, {
            Hash = Config.Cocaine.StartPos[k].ped,
            Location = Config.Cocaine.StartPos[k].coords - Config.Cocaine.StartPos[k].offset,
            Scenario = Config.Cocaine.StartPos[k].scenario
        })
        TMC.Functions.DisablePed('cokestartped' .. k)
        TMC.Functions.AddCircleZone('cokestartpedint', TMC.Natives.GetOffsetFromCoordsInDirection(Config.Cocaine.StartPos[k].coords.xyz, Config.Cocaine.StartPos[k].coords.w, vector3(0.0, 0.0, 0.0)), 1.0, {useZ = true, data = {index = k}})
    end

    TMC.Functions.AddPolyZoneEnterHandler('cokestartpedint', function(data)
        if curMissionGiver and curMissionGiver == data.index then
            if inQueue then
                 TMC.Functions.ShowPromptGroup(cokeStartPedPrompt, {'stop'})
            else
                 TMC.Functions.ShowPromptGroup(cokeStartPedPrompt, {'start'})
            end
        end
     end)

    TMC.Functions.AddPolyZoneExitHandler('cokestartpedint', function(data)
        TMC.Functions.HidePromptGroup(cokeStartPedPrompt)
    end)

    cokeStartPedPrompt = TMC.Functions.CreatePromptGroup({
        {
            Id = 'start',
            Complete = function()
                if not HasDropOff and not inQueue then
                    inQueue = true
                    TMC.Functions.SimpleNotify("We'll be in touch.", 'speech')
                    Citizen.Wait(600)
                    TMC.Functions.TriggerServerEvent('drugs:cokequeue', 'joinqueue')
                    Citizen.Wait(600)
                    --print('inqueue: ', inQueue)
                end
            end,
            Title = 'Start Chatter',
            AutoComplete = true,
            Description = 'Chat with.',
            Icon = 'fa-duotone fa-message-dots'
        },
        {
            Id = 'stop',
            Complete = function()
                if inQueue then
                    TMC.Functions.TriggerServerEvent('drugs:cokequeue', 'leavequeue')
                    HasDropOff = nil
                    inQueue = false
                    BlipCleanFunction('DropOff')
                    BinCleanUp()
                    Citizen.Wait(300)
                    --print('inqueue: ', inQueue)
                end
            end,
            Title = 'Start Chatter',
            AutoComplete = true,
            Description = 'Chat with.',
            Icon = 'fa-duotone fa-message-dots'
        }
    })

    TMC.Functions.AddPolyZoneEnterHandler('cokedropoffint', function(data)
        if HasDropOff then
            TMC.Functions.ShowPromptGroup(cokeDropOffPedPrompt)
        end
    end)

    TMC.Functions.AddPolyZoneExitHandler('cokedropoffint', function(data)
        TMC.Functions.HidePromptGroup(cokeDropOffPedPrompt)
    end)

    cokeDropOffPedPrompt = TMC.Functions.CreatePromptGroup({
        {
            Id = 'deliver',
            Complete = function()
                local amount = TMC.Functions.GetItemAmountByName('cash_roll')
                local amountNeeded = 50

                if amount >= amountNeeded then
                    TMC.Functions.TriggerServerEvent('TMC:Server:RemoveItem', 'cash_roll', amountNeeded)
                    TMC.Functions.LoadAnimDict('pickup_object')
                    TaskPlayAnim(playerPedId, 'pickup_object', 'pickup_low', 8.0, -8.0, 2000, 1, 0, false, false, false)
                    Wait(2000)
                    ClearPedTasksImmediately(playerPedId)
                    dropOffSuccess = true
                    HasDropOff = false
                    inQueue = false
                    TMC.Functions.HidePromptGroup(cokeDropOffPedPrompt)
                    TMC.Functions.SimpleNotify('I\'ll let the boss man know you\'ve dropped off the package. He should let you know the information shortly.', 'speech')
                    TMC.Functions.TriggerServerEvent('drugs:cokegivefield', playerPedId, true)
                else
                    TMC.Functions.SimpleNotify('You don\'t have the required materials. The boss isn\'t gonna be happy. Get outta here!', 'speech', 1000 * 5)
                    TMC.Functions.TriggerServerEvent('drugs:cokequeue', 'leavequeue')
                    dropOffSuccess = false
                    HasDropOff = false
                    inQueue = false
                    TMC.Functions.HidePromptGroup(cokeDropOffPedPrompt)
                end
                BlipCleanFunction('DropOff')
                BinCleanUp()
            end,
            Title = 'Deliever the Down Payment',
            AutoComplete = true,
            Icon = 'fa-duotone fa-box'
        }
    })

    for k,v in pairs(Config.Cocaine.Zones) do
        cokefield[k] = TMC.Functions.AddPolyZone('cokefield'..k, v.coords, {
            minZ = v.minZ,
            maxZ = v.maxZ,
            data = {
                field = k
            }
        })

        TMC.Functions.AddPolyZoneEnterHandler('cokefield'..k, function(data)
            inField = data.field
            if inField == fieldSelected then
                fieldAssigned = false
            end

            local maxFields = 0
            for k,v in pairs(Config.Cocaine['Options']['MaxHarvestZones'][inField]) do
                if v[1] >= harvestrep then
                    maxFields = v[2]
                    break
                end
            end
            TMC.Functions.TriggerServerCallback('drugs:cokeFieldAssignedCallback', function(result)
                if result == data.field then
                    local fieldCounts = 0
                    TMC.Functions.TriggerServerCallback('drugs:cokeFieldCountCallback', function(result)
                        fieldCounts = result
                        --print ("callback successful")
                    end, inField)
                    --print("fieldCounts, ", fieldCounts)
                    if maxFields - fieldCounts > 10 then
                        TMC.Functions.TriggerServerEvent('drugs:stopfieldtimer', data.field)
                        if TMC.IsDev then
                            print("field timeout has been canceled, if started")
                        end
                    end
                end
            end)
            TMC.Functions.SimpleNotify('Field #' .. inField .. '.', 'info', 1000 * 7)
            TMC.Functions.TriggerServerEvent('drugs:cokezoneregister', data.field, true)
            --print("back to client from cokezoneregister")
            Citizen.Wait(300)
            local hasShears = TMC.Functions.HasItem('plantshears')
            if hasShears then
                if not isHarvesting then
                    TMC.Functions.ShowPromptGroup(cokeFieldPromptGroup)
                end
            end
            curCokeYard = k
			CokeFieldTick()
        end)


        TMC.Functions.AddPolyZoneExitHandler('cokefield'..k, function(data)
            inField = nil
            finishCollection = false
            TMC.Functions.TriggerServerEvent('drugs:cokezoneregister', data.field, false)
            TMC.Functions.HidePromptGroup(cokeFieldPromptGroup)
            --print("Left Zone: "..k)
            curCokeYard = false
            curCokeYardData = nil
            TMC.Functions.TriggerServerCallback('drugs:cokeFieldAssignedCallback', function(result)
                if result == data.field then
                    TMC.Functions.TriggerServerEvent('drugs:cokeresetfieldtimer', data.field, true)
                end
            end)
        end)
    end
    cokeFieldsCreated = true

    cokeFieldPromptGroup = TMC.Functions.CreatePromptGroup({
        {
            Id = 'harvest',
            Complete = function()
                if not TMC.Functions.IsDead() then
                    TMC.Functions.HidePromptGroup(cokeFieldPromptGroup)
                    HarvestPlantsCheck()
                end
            end,
            Title = 'Start to Harvest',
            Description = 'Probably not a good idea to touch these plants without permission!',
            AutoComplete = false,
            Icon = 'fa-duotone fa-leaf'
        }
    })

    for k,v in pairs(Config.Cocaine.Labs) do
        TMC.Functions.AddPolyZone('cokelab'..k, v.Area.points, {
            minZ = v.Area.options.minZ,
            maxZ = v.Area.options.maxZ,
            data = {
                lab = k,
                modifier = v.ProductQualityMultiplier
            }
        })

        TMC.Functions.AddPolyZoneEnterHandler('cokelab'..k, function(data)
            cokelab = data.lab
            cokelabgrade = data.modifier
        end)

        TMC.Functions.AddPolyZoneExitHandler('cokelab'..k, function(data)
            cokelab = nil
            cokelabgrade = nil
            TMC.Functions.TriggerServerEvent('cokelabregister', data.lab, false)
        end)

        local cokeLabEntryExit = nil
        TMC.Functions.AddCircleZone('cokelabentry'..k, v.EnterDoorCoords, 1.0, {
            useZ = true,
            data = {
                lab = k
            }
        })

        TMC.Functions.AddPolyZoneEnterHandler('cokelabentry'..k, function(data)
            if v.Unlocked then
                TMC.Functions.ShowPromptGroup(cokeLabEntryExit, {'entry'})
                generateCokeLabFunctions(k)
            elseif hasItemwithInfo('coke', k) then
                TMC.Functions.ShowPromptGroup(cokeLabEntryExit, {'entry', 'unlock'})
                generateCokeLabFunctions(k)
            end
        end)

        TMC.Functions.AddPolyZoneExitHandler('cokelabentry'..k, function(data)
            TMC.Functions.HidePromptGroup(cokeLabEntryExit)
        end)

        TMC.Functions.AddCircleZone('cokelabexit'..k, v.ExitDoorCoords, 1.0, {
            useZ = true,
            data = {
                lab = k
            }
        })

        TMC.Functions.AddPolyZoneEnterHandler('cokelabexit'..k, function(data)
            if hasItemwithInfo('coke', k) then
                TMC.Functions.ShowPromptGroup(cokeLabEntryExit, {'exit', 'unlock'})
            else
                TMC.Functions.ShowPromptGroup(cokeLabEntryExit, {'exit'})
            end
        end)

        TMC.Functions.AddPolyZoneExitHandler('cokelabexit'..k, function(data)
            TMC.Functions.HidePromptGroup(cokeLabEntryExit)
        end)

        TMC.Functions.AddCircleZone('cokelabstash'..k, v.StashLocation, 1.0, {
            useZ = true,
            data = {
                lab = k
            }
        })

        local cokeLabStash = nil

        TMC.Functions.AddPolyZoneEnterHandler('cokelabstash'..k, function(data)
            TMC.Functions.ShowPromptGroup(cokeLabStash)
        end)

        TMC.Functions.AddPolyZoneExitHandler('cokelabstash'..k, function(data)
            TMC.Functions.HidePromptGroup(cokeLabStash)
        end)

        cokeLabStash = TMC.Functions.CreatePromptGroup({
            {
                Id = 'access',
                Complete = function()
                    TMC.Functions.TriggerServerEvent('inventory:server:openInventory', 'stash', "cokelab_" .. k .. "_stash", {
                        title = Config.Cocaine.Labs[k].Name .." Stash",
                        slotCount = 30,
                        maxWeight = 800000,
                        temp = false
                    })
                end,
                Title = 'Enter',
                AutoComplete = true,
                Icon = 'fa-duotone fa-sack'
            },
        })

        cokeLabEntryExit = TMC.Functions.CreatePromptGroup({
            {
                Id = 'entry',
                Complete = function()
                    for id, barrel in pairs(v.BarrelSpawn) do
                        local barrel = TMC.Functions.SpawnLocalObject('prop_barrel_03a', barrel)
                        FreezeEntityPosition(barrel, true)
                        table.insert(barrelTable, barrel)
                    end
                    TMC.Functions.TeleportToCoords(v.ExitDoorTPCoords, false)
                    GetZonePedIsInTick()
                end,
                Title = 'Enter',
                AutoComplete = true,
                Icon = 'fa-duotone fa-door-open'
            },
            {
                Id = 'exit',
                Complete = function()
                    for id, barrel in pairs(barrelTable) do
                        TMC.Functions.DeleteEntity(barrel)
                        barrelTable[id] = nil
                    end
                    TMC.Functions.TeleportToCoords(v.EnterDoorTPCoords, false)
                end,
                Title = 'Exit',
                AutoComplete = true,
                Icon = 'fa-duotone fa-door-open'
            },
            {
                Id = "unlock",
                Complete = function()
                    if hasItemwithInfo('coke', k) then
                        TMC.Functions.TriggerServerEvent('drugs:cokeunlock', k, not v.Unlocked)
                        local status = 'Locked'
                        if v.Unlocked then status = 'Unlocked' end
                        TMC.Functions.SimpleNotify('Lab has been ' .. status, 'info')
                    end
                end,
                Title = "Toggle Lab Lock Status",
                Icon = 'fa-duotone fa-lock'
            }
        })
    end
    -- Glove Check
    function IsWearingGloves()
        local armIndex = GetPedDrawableVariation(PlayerPedId(), 3)
        local model = GetEntityModel(PlayerPedId())
        local retval = true
        if model == GetHashKey("mp_m_freemode_01") then
            if Config.MaleNoGloves[armIndex] ~= nil and Config.MaleNoGloves[armIndex] then
                retval = false
            end
        else
            if Config.FemaleNoGloves[armIndex] ~= nil and Config.FemaleNoGloves[armIndex] then
                retval = false
            end
        end
        return retval
    end

    function clothingCheckTick()
        if (not IsWearingGloves() or not IsWearingMask()) then
            TMC.Functions.SimpleNotify('You should probably be wearing some protection...', 'info', 1000 * 6)
        end
        Citizen.CreateThread(function()
            while isProcessing do
                if not IsWearingGloves() then
                    SetEntityHealth(PlayerPedId(), GetEntityHealth(PlayerPedId()) - 5)
                end
                if not IsWearingMask() then
                    SetEntityHealth(PlayerPedId(), GetEntityHealth(PlayerPedId()) - 5)
                end
                Citizen.Wait(5000)
            end
        end)
    end

    cokeLabSoakPrompt = TMC.Functions.CreatePromptGroup({
        {
            Id = 'soak_prompt',
            Complete = function()
                LabSoak()
            end,
            Title = 'Start Soaking Process',
            AutoComplete = true,
            Icon = 'fa-duotone fa-fill'
        }
    })

    cokeLabPrepPrompt = TMC.Functions.CreatePromptGroup({
        {
            Id = 'start_process',
            Complete = function()
                if TMC.Functions.HasItem('cokeleaf_paste') then
                    LabPrep()
                else
                    TMC.Functions.SimpleNotify('It seems like you don\'t have the necessary ingredient to continue', 'error', 1000 * 5)
                end
            end,
            Title = 'Start Preparation',
            AutoComplete = true,
            Icon = 'fa-duotone fa-knife-kitchen'
        },
    })

    cokeLabCombinePrompt = TMC.Functions.CreatePromptGroup({
        {
            Id = 'start_process',
            Complete = function()
                if TMC.Functions.HasItem('cokechemicalslush') then
                    LabCombine()
                else
                    TMC.Functions.SimpleNotify('It seems like you don\'t have the necessary ingredient to continue', 'error', 1000 * 5)
                end
            end,
            Title = 'Start Combination Process',
            AutoComplete = true,
            Icon = 'fa-duotone fa-bowl-spoon'
        },
    })

    if productionrep <= 100 then
        cokeLabBagPrompt = TMC.Functions.CreatePromptGroup({
            {
                Id = 'start_process_single_baggie',
                Complete = function()
                    if TMC.Functions.HasItem('rawcoke') and TMC.Functions.HasItem('empty_weed_bag') then
                        startLabBag('single_bag')
                    else
                        TMC.Functions.SimpleNotify('It seems like you don\'t have enough Raw Product and Empty Baggies', 'error', 1000 * 5)
                    end
                end,
                Title = 'Bag Product - 1 Raw Coke',
                AutoComplete = true,
                Icon = 'fa-duotone fa-box'
            },
            {
                Id = 'start_process_bag',
                Complete = function()
                    if TMC.Functions.HasItem('coke_brick') and TMC.Functions.GetItemAmountByName('plastic_baggies') >= 1 then
                        startLabBag('bag')
                    else
                        TMC.Functions.SimpleNotify('It seems like you don\'t have enough Bricks and Baggies to Bag this product', 'error', 1000 * 5)
                    end
                end,
                Title = 'Bag Product - Brick',
                AutoComplete = true,
                Icon = 'fa-duotone fa-box'
            },
        })
    elseif productionrep <= 200 then
        cokeLabBagPrompt = TMC.Functions.CreatePromptGroup({
            {
                Id = 'start_process_single_baggie',
                Complete = function()
                    if TMC.Functions.HasItem('rawcoke') and TMC.Functions.HasItem('empty_weed_bag') then
                        startLabBag('single_bag')
                    else
                        TMC.Functions.SimpleNotify('It seems like you don\'t have enough Raw Product and Empty Baggies', 'error', 1000 * 5)
                    end
                end,
                Title = 'Bag Product - 1 Raw Coke',
                AutoComplete = true,
                Icon = 'fa-duotone fa-box'
            },
            {
                Id = 'start_process_five_baggie',
                Complete = function()
                    if TMC.Functions.HasItem('rawcoke') and TMC.Functions.HasItem('empty_weed_bag') then
                        startLabBag('five_bags')
                    else
                        TMC.Functions.SimpleNotify('It seems like you don\'t have enough Raw Product and Empty Baggies', 'error', 1000 * 5)
                    end
                end,
                Title = 'Bag Product - 5 Raw Coke',
                AutoComplete = true,
                Icon = 'fa-duotone fa-box'
            },
            {
                Id = 'start_process_bag',
                Complete = function()
                    if TMC.Functions.HasItem('coke_brick') and TMC.Functions.GetItemAmountByName('plastic_baggies') >= 1 then
                        startLabBag('bag')
                    else
                        TMC.Functions.SimpleNotify('It seems like you don\'t have enough Bricks and Baggies to Bag this product', 'error', 1000 * 5)
                    end
                end,
                Title = 'Bag Product - Brick',
                AutoComplete = true,
                Icon = 'fa-duotone fa-box'
            },
        })

    elseif productionrep <= 300 then
        cokeLabBagPrompt = TMC.Functions.CreatePromptGroup({
            {
                Id = 'start_process_single_baggie',
                Complete = function()
                    if TMC.Functions.HasItem('rawcoke') and TMC.Functions.HasItem('empty_weed_bag') then
                        startLabBag('single_bag')
                    else
                        TMC.Functions.SimpleNotify('It seems like you don\'t have enough Raw Product and Empty Baggies', 'error', 1000 * 5)
                    end
                end,
                Title = 'Bag Product - 1 Raw Coke',
                AutoComplete = true,
                Icon = 'fa-duotone fa-box'
            },
            {
                Id = 'start_process_five_baggie',
                Complete = function()
                    if TMC.Functions.HasItem('rawcoke') and TMC.Functions.HasItem('empty_weed_bag') then
                        startLabBag('five_bags')
                    else
                        TMC.Functions.SimpleNotify('It seems like you don\'t have enough Raw Product and Empty Baggies', 'error', 1000 * 5)
                    end
                end,
                Title = 'Bag Product - 5 Raw Coke',
                AutoComplete = true,
                Icon = 'fa-duotone fa-box'
            },
            {
                Id = 'start_process_ten_baggie',
                Complete = function()
                    if TMC.Functions.HasItem('rawcoke') and TMC.Functions.HasItem('empty_weed_bag') then
                        startLabBag('ten_bags')
                    else
                        TMC.Functions.SimpleNotify('It seems like you don\'t have enough Raw Product and Empty Baggies', 'error', 1000 * 5)
                    end
                end,
                Title = 'Bag Product - 10 Raw Coke',
                AutoComplete = true,
                Icon = 'fa-duotone fa-box'
            },
            {
                Id = 'start_process_bag',
                Complete = function()
                    if TMC.Functions.HasItem('coke_brick') and TMC.Functions.GetItemAmountByName('plastic_baggies') >= 1 then
                        startLabBag('bag')
                    else
                        TMC.Functions.SimpleNotify('It seems like you don\'t have enough Bricks and Baggies to Bag this product', 'error', 1000 * 5)
                    end
                end,
                Title = 'Bag Product - Brick',
                AutoComplete = true,
                Icon = 'fa-duotone fa-box'
            },
        })
    elseif productionrep <= 400 then
        cokeLabBagPrompt = TMC.Functions.CreatePromptGroup({
            {
                Id = 'start_process_single_baggie',
                Complete = function()
                    if TMC.Functions.HasItem('rawcoke') and TMC.Functions.HasItem('empty_weed_bag') then
                        startLabBag('single_bag')
                    else
                        TMC.Functions.SimpleNotify('It seems like you don\'t have enough Raw Product and Empty Baggies', 'error', 1000 * 5)
                    end
                end,
                Title = 'Bag Product - 1 Raw Coke',
                AutoComplete = true,
                Icon = 'fa-duotone fa-box'
            },
            {
                Id = 'start_process_five_baggie',
                Complete = function()
                    if TMC.Functions.HasItem('rawcoke') and TMC.Functions.HasItem('empty_weed_bag') then
                        startLabBag('five_bags')
                    else
                        TMC.Functions.SimpleNotify('It seems like you don\'t have enough Raw Product and Empty Baggies', 'error', 1000 * 5)
                    end
                end,
                Title = 'Bag Product - 5 Raw Coke',
                AutoComplete = true,
                Icon = 'fa-duotone fa-box'
            },
            {
                Id = 'start_process_ten_baggie',
                Complete = function()
                    if TMC.Functions.HasItem('rawcoke') and TMC.Functions.HasItem('empty_weed_bag') then
                        startLabBag('ten_bags')
                    else
                        TMC.Functions.SimpleNotify('It seems like you don\'t have enough Raw Product and Empty Baggies', 'error', 1000 * 5)
                    end
                end,
                Title = 'Bag Product - 10 Raw Coke',
                AutoComplete = true,
                Icon = 'fa-duotone fa-box'
            },
            {
                Id = 'start_process_15_baggie',
                Complete = function()
                    if TMC.Functions.HasItem('rawcoke') and TMC.Functions.HasItem('empty_weed_bag') then
                        startLabBag('fifteen_bags')
                    else
                        TMC.Functions.SimpleNotify('It seems like you don\'t have enough Raw Product and Empty Baggies', 'error', 1000 * 5)
                    end
                end,
                Title = 'Bag Product - 15 Raw Coke',
                AutoComplete = true,
                Icon = 'fa-duotone fa-box'
            },
            {
                Id = 'start_process_bag',
                Complete = function()
                    if TMC.Functions.HasItem('coke_brick') and TMC.Functions.GetItemAmountByName('plastic_baggies') >= 1 then
                        startLabBag('bag')
                    else
                        TMC.Functions.SimpleNotify('It seems like you don\'t have enough Bricks and Baggies to Bag this product', 'error', 1000 * 5)
                    end
                end,
                Title = 'Bag Product - Brick',
                AutoComplete = true,
                Icon = 'fa-duotone fa-box'
            },
        })
    elseif productionrep <= 500 then
        cokeLabBagPrompt = TMC.Functions.CreatePromptGroup({
            {
                Id = 'start_process_single_baggie',
                Complete = function()
                    if TMC.Functions.HasItem('rawcoke') and TMC.Functions.HasItem('empty_weed_bag') then
                        startLabBag('single_bag')
                    else
                        TMC.Functions.SimpleNotify('It seems like you don\'t have enough Raw Product and Empty Baggies', 'error', 1000 * 5)
                    end
                end,
                Title = 'Bag Product - 1 Raw Coke',
                AutoComplete = true,
                Icon = 'fa-duotone fa-box'
            },
            {
                Id = 'start_process_five_baggie',
                Complete = function()
                    if TMC.Functions.HasItem('rawcoke') and TMC.Functions.HasItem('empty_weed_bag') then
                        startLabBag('five_bags')
                    else
                        TMC.Functions.SimpleNotify('It seems like you don\'t have enough Raw Product and Empty Baggies', 'error', 1000 * 5)
                    end
                end,
                Title = 'Bag Product - 5 Raw Coke',
                AutoComplete = true,
                Icon = 'fa-duotone fa-box'
            },
            {
                Id = 'start_process_ten_baggie',
                Complete = function()
                    if TMC.Functions.HasItem('rawcoke') and TMC.Functions.HasItem('empty_weed_bag') then
                        startLabBag('ten_bags')
                    else
                        TMC.Functions.SimpleNotify('It seems like you don\'t have enough Raw Product and Empty Baggies', 'error', 1000 * 5)
                    end
                end,
                Title = 'Bag Product - 10 Raw Coke',
                AutoComplete = true,
                Icon = 'fa-duotone fa-box'
            },
            {
                Id = 'start_process_15_baggie',
                Complete = function()
                    if TMC.Functions.HasItem('rawcoke') and TMC.Functions.HasItem('empty_weed_bag') then
                        startLabBag('fifteen_bags')
                    else
                        TMC.Functions.SimpleNotify('It seems like you don\'t have enough Raw Product and Empty Baggies', 'error', 1000 * 5)
                    end
                end,
                Title = 'Bag Product - 15 Raw Coke',
                AutoComplete = true,
                Icon = 'fa-duotone fa-box'
            },
            {
                Id = 'start_process_25_baggie',
                Complete = function()
                    if TMC.Functions.HasItem('rawcoke') and TMC.Functions.HasItem('empty_weed_bag') then
                        startLabBag('25_bags')
                    else
                        TMC.Functions.SimpleNotify('It seems like you don\'t have enough Raw Product and Empty Baggies', 'error', 1000 * 5)
                    end
                end,
                Title = 'Bag Product - 25 Raw Coke',
                AutoComplete = true,
                Icon = 'fa-duotone fa-box'
            },
            {
                Id = 'start_process_bag',
                Complete = function()
                    if TMC.Functions.HasItem('coke_brick') and TMC.Functions.GetItemAmountByName('plastic_baggies') >= 1 then
                        startLabBag('bag')
                    else
                        TMC.Functions.SimpleNotify('It seems like you don\'t have enough Bricks and Baggies to Bag this product', 'error', 1000 * 5)
                    end
                end,
                Title = 'Bag Product - Brick',
                AutoComplete = true,
                Icon = 'fa-duotone fa-box'
            },
        })
    else
        cokeLabBagPrompt = TMC.Functions.CreatePromptGroup({
            {
                Id = 'start_process_single_baggie',
                Complete = function()
                    if TMC.Functions.HasItem('rawcoke') and TMC.Functions.HasItem('empty_weed_bag') then
                        startLabBag('single_bag')
                    else
                        TMC.Functions.SimpleNotify('It seems like you don\'t have enough Raw Product and Empty Baggies', 'error', 1000 * 5)
                    end
                end,
                Title = 'Bag Product - 1 Raw Coke',
                AutoComplete = true,
                Icon = 'fa-duotone fa-box'
            },
            {
                Id = 'start_process_five_baggie',
                Complete = function()
                    if TMC.Functions.HasItem('rawcoke') and TMC.Functions.HasItem('empty_weed_bag') then
                        startLabBag('five_bags')
                    else
                        TMC.Functions.SimpleNotify('It seems like you don\'t have enough Raw Product and Empty Baggies', 'error', 1000 * 5)
                    end
                end,
                Title = 'Bag Product - 5 Raw Coke',
                AutoComplete = true,
                Icon = 'fa-duotone fa-box'
            },
            {
                Id = 'start_process_ten_baggie',
                Complete = function()
                    if TMC.Functions.HasItem('rawcoke') and TMC.Functions.HasItem('empty_weed_bag') then
                        startLabBag('ten_bags')
                    else
                        TMC.Functions.SimpleNotify('It seems like you don\'t have enough Raw Product and Empty Baggies', 'error', 1000 * 5)
                    end
                end,
                Title = 'Bag Product - 10 Raw Coke',
                AutoComplete = true,
                Icon = 'fa-duotone fa-box'
            },
            {
                Id = 'start_process_15_baggie',
                Complete = function()
                    if TMC.Functions.HasItem('rawcoke') and TMC.Functions.HasItem('empty_weed_bag') then
                        startLabBag('fifteen_bags')
                    else
                        TMC.Functions.SimpleNotify('It seems like you don\'t have enough Raw Product and Empty Baggies', 'error', 1000 * 5)
                    end
                end,
                Title = 'Bag Product - 15 Raw Coke',
                AutoComplete = true,
                Icon = 'fa-duotone fa-box'
            },
            {
                Id = 'start_process_25_baggie',
                Complete = function()
                    if TMC.Functions.HasItem('rawcoke') and TMC.Functions.HasItem('empty_weed_bag') then
                        startLabBag('25_bags')
                    else
                        TMC.Functions.SimpleNotify('It seems like you don\'t have enough Raw Product and Empty Baggies', 'error', 1000 * 5)
                    end
                end,
                Title = 'Bag Product - 25 Raw Coke',
                AutoComplete = true,
                Icon = 'fa-duotone fa-box'
            },
            {
                Id = 'start_process_brick',
                Complete = function()
                    if TMC.Functions.HasItem('rawcoke') and TMC.Functions.HasItem('packingbox') then
                        startLabBag('box')
                    else
                        TMC.Functions.SimpleNotify('It seems like you don\'t have enough Raw Product and Packing Boxes', 'error', 1000 * 5)
                    end
                end,
                Title = 'Box Product - Raw Coke',
                AutoComplete = true,
                Icon = 'fa-duotone fa-box'
            },
            {
                Id = 'start_process_bag',
                Complete = function()
                    if TMC.Functions.HasItem('coke_brick') and TMC.Functions.GetItemAmountByName('plastic_baggies') >= 1 then
                        startLabBag('bag')
                    else
                        TMC.Functions.SimpleNotify('It seems like you don\'t have enough Bricks and Baggies to Bag this product', 'error', 1000 * 5)
                    end
                end,
                Title = 'Bag Product - Brick',
                AutoComplete = true,
                Icon = 'fa-duotone fa-box'
            },
        })
    end

    while not GlobalState.CocaineStartPed do
        Citizen.Wait(1000)
    end
    curMissionGiver = GlobalState.CocaineStartPed
    TMC.Functions.EnablePed("cokestartped"..curMissionGiver)
end)

AddStateBagChangeHandler("CocaineStartPed", 'global', function(bagName, keyName, newVal)
    if newVal ~= curMissionGiver then
        TMC.Functions.EnablePed("cokestartped"..newVal)
        TMC.Functions.DisablePed("cokestartped"..curMissionGiver)
    end
    curMissionGiver = newVal
end)

local randomDelay = 0
function GiveDropOff()
    if not HasDropOff then
        HasDropOff = true
        randomDelay = TMC.Common.TrueRandom(1000 * 30, 1000 * 60 * 2)
        --randomDelay = 1000 -- FOR DEV TESTING ONLY
        dropOffArea = TMC.Common.TrueRandom(1, #Config.Cocaine.DropOffLocations)
        dropOffBin = TMC.Common.TrueRandom(1, #Config.Cocaine.DropOffLocations[dropOffArea].bins)
        SetTimeout(randomDelay, function()
            TMC.Functions.TriggerServerEvent('drugs:cokesenddroplocation', dropOffArea)
			randomDelay = 0
            DropOffBinSpawn()
		end)
    end
end

function DropOffBinSpawn()
    --print("DEBUG == " .. dropOffArea .. "   -   " .. dropOffBin)
    local bin = Config.Cocaine.DropOffLocations[dropOffArea].bins[dropOffBin]
    --bin.coords = vector4(297.36074, -1715.556, 29.169458, 321.4635)
    if TMC.IsDev then
        print("bin coords", bin.coords)
    end

    dropOffPZ = TMC.Functions.AddCircleZone('cokedropoffspawn', bin.coords, 70, {
        useZ = false,
        data = {
            bin = dropOffBin,
            coords = bin.coords
        }
    })

    TMC.Functions.AddPolyZoneEnterHandler('cokedropoffspawn', function(data)
        local startBinInt = TMC.Natives.GetOffsetFromCoordsInDirection(bin.coords.xyz, bin.coords.w, vector3(0.0, -1.0, 0.0))
        DropOffBinObject = CreateObject(bin.model, bin.coords.x, bin.coords.y - bin.offset.y, bin.coords.z, true, true)
        while not DropOffBinObject do
            Citizen.Wait(5)
        end
        SetEntityHeading(DropOffBinObject, bin.coords.w)
        PlaceObjectOnGroundProperly(DropOffBinObject)

        dropOffBinZone = TMC.Functions.AddCircleZone('cokedropoffint', startBinInt, 1.0, {
            useZ = true,
            data = {
                bin = dropOffBin,
                coords = bin.coords
            }
        })
        TMC.Functions.RemoveZoneById(dropOffPZ.id)
    end)


    for k,v in pairs(Config.Cocaine.DropOffLocations[dropOffArea].blips) do
        local tempBlip = AddBlipForRadius(v.coords.xyz, v.radius)
        SetBlipHighDetail(tempBlip, true)
        SetBlipColour(tempBlip, 1)
        SetBlipAlpha (tempBlip, 128)
        local data = {
            blip = tempBlip,
            category = 'DropOff'
        }
        table.insert(blipTable, data)
    end
end

function BlipCleanFunction(category)
    if category ~= nil then
        for k,v in pairs(blipTable) do
            if v.category == category then
                RemoveBlip(v.blip)
                blipTable[k] = nil
            end
        end
    else
        for k,v in pairs(blipTable) do
            RemoveBlip(v.blip)
            blipTable[k] = nil
        end
    end
end

function CokeFieldTick()
	Citizen.CreateThread(function()
		local plyPed = PlayerPedId()

        local harvestWidth = 0

        for k,v in pairs(Config.Cocaine['Options']['HarvestWidth']) do
            if v[1] >= harvestrep then
                harvestWidth = v[2]
                if TMC.IsDev then
                    print("harvest rep:", harvestrep)
                    --print("k:", k)
                    print('harvestWidth value:', v[2])
                end
                break
            end
        end

		while curCokeYard do
			local plyPos = GetEntityCoords(plyPed)
			for k,v in pairs(CokeFieldData[curCokeYard]) do
				local r,g,b = 0,0,0
				DrawMarker(1, v.pos.x, v.pos.y, v.pos.z-1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, harvestWidth, harvestWidth, 0.7, CokeMarkerColour[v.status].r, CokeMarkerColour[v.status].g, CokeMarkerColour[v.status].b, 180, false, true, 0, false)
				if v.status == "harvested" then

				elseif v.status == "pending" then
				elseif v.status == "processing" then
				end
			end
			if curHarvest then
				local dist = #(plyPos - curHarvest)
				if dist > 2.0 then
                    TMC.Functions.StopNotify('CokeHarvest')
					TMC.Functions.TriggerServerEvent('drugs:cokeleavePendingHarvest', curCokeYard, curHarvest)
					curHarvest = false
				end
			end
			Citizen.Wait(0)
		end
		if not curCokeYard and curHarvest then
            TMC.Functions.StopNotify('CokeHarvest')
			TMC.Functions.TriggerServerEvent('drugs:cokeleavePendingHarvest', curCokeYard, curHarvest)
			curHarvest = false
		end
	end)
end

RegisterNetEvent("drugs:cokeStartGroupHarvest", function(pos, amount) -- ld code
	TMC.Functions.StopNotify("CokeHarvest")
    --temporary values for zone testing.
    ClearPedTasks(PlayerPedId())
	--TMC.Functions.TriggerServerEvent("drugs:cokecompleteHarvest", curCokeYard, pos, amount)
    TMC.Functions.LoadAnimDict('amb@world_human_gardener_plant@male@base')
    TaskPlayAnim(playerPedId, "amb@world_human_gardener_plant@male@base", "base", 8.0, -8.0, 1000 * 10, 2, 0, false, false, false)
    TMC.Functions.ProgressBar(function(success)
        if success then
			ClearPedTasks(PlayerPedId())
			TMC.Functions.TriggerServerEvent("drugs:cokecompleteHarvest", curCokeYard, pos, amount)
            randomRepChance('cokeharvest', 15)
            Citizen.Wait(500)
        else
            ClearPedTasks(PlayerPedId())
			TMC.Functions.SimpleNotify("Cancelled", "error")
        end
        StopAnimTask(playerPedId, 'amb@world_human_gardener_plant@male@base')
        TMC.Functions.ShowPromptGroup(cokeFieldPromptGroup)
    end, 1000 * 10, "Harvesting", "Snipping some leaves")
end)

function HarvestPlantsCheck()
    local has4 = false
    local hasAccess = false
    isHarvesting = true
    TMC.Functions.TriggerServerCallback('drugs:cokezonecallback', function(result)
        has4 = result.success
        hasAccess = result.hasAccess
        if has4 then
            if hasAccess then
                HarvestPlants(true, result.agro)
            elseif not hasAccess then
                HarvestPlants(false, result.agro)
            end
        else
            TMC.Functions.SimpleNotify('You need more people to assist you with this...', 'info')
            isHarvesting = false
            TMC.Functions.ShowPromptGroup(cokeFieldPromptGroup)
        end
    end, inField)
end

function HarvestPlants(hasAccess, agro)
    local plyPos = GetEntityCoords(playerPedId)
    local leafamount = 0
    for k,v in pairs(Config.Cocaine['Options']['LeafGain']) do
        if v[1] >= harvestrep then
            leafamount = TMC.Common.TrueRandom(v[2], v[3])
            if TMC.IsDev then
                print("harvest rep:", harvestrep)
                --print("k:", k)
                print('leaf amount values:', v[2], v[3])
            end
            break
        end
    end

    if curHarvest then return; end
    TMC.Functions.TriggerServerCallback('drugs:cokeleafcallback', function(result)
        if result.success == true then
            TMC.Functions.TriggerServerEvent("drugs:cokeAttemptLeafClip", plyPos, inField, leafamount)
            finishCollection = result.finish
        else
            TMC.Functions.SimpleNotify('Looks like this field has run out of product', 'error')
        end
    end, inField, leafamount, finishCollection, plyPos)
    Citizen.Wait(300)
    isHarvesting = false
    --print("finishCollection", finishCollection)
    if finishCollection then
        fieldAccessGranted = false
        fieldSelected = nil
        local message = 'Hey, it looks like we are done here. My people tell me your harvested the entire field. Good job!'
        phoneNotify(message, 1000 * 15)
    end

    if not hasAccess then SpawnCayoCokeGuards(agro) end
end



function SpawnCayoCokeGuards(agro)
    local amount = 0

    if agro == 1 then
        amount = TMC.Common.TrueRandom(1, 4)
    elseif agro == 2 then
        amount = TMC.Common.TrueRandom(6, 8)
    else
        amount = TMC.Common.TrueRandom(15, 15)
    end

    local pedkey = {}
    for i = 1, amount do
        local randomNum = TMC.Common.TrueRandom(1, #Config.Cocaine.CayoGuards)
        table.insert(pedkey, randomNum)
    end

    local combatpeds = {}
    local combatpedsgroup = GetHashKey("HATES_PLAYER")
    local plyHash = GetHashKey("PLAYER")
    SetPedRelationshipGroupHash(playerPedId, plyHash)
    SetPedRelationshipGroupDefaultHash(playerPedId, plyHash)
    SetRelationshipBetweenGroups(5, combatpedsgroup, plyHash)
    SetRelationshipBetweenGroups(5, plyHash, combatpedsgroup)
    for k,v in pairs(pedkey) do
        local pedinfo = Config.Cocaine.CayoGuards[v]
        TMC.Functions.LoadModel(pedinfo.Ped)

        local ped = CreatePed(1, pedinfo.Ped, pedinfo.Coords.x, pedinfo.Coords.y, pedinfo.Coords.z, pedinfo.Coords.w, true, true)
        GiveWeaponToPed(ped, pedinfo.Weapon, 100, false, true)
        SetPedDropsWeaponsWhenDead(ped, false)
        SetPedMaxHealth(ped, 500)
        SetEntityHealth(ped, 500)
        SetPedArmour(ped, 100)
        SetPedRelationshipGroupHash(ped, combatpedsgroup)
        SetPedRelationshipGroupDefaultHash(ped, combatpedsgroup)
        SetPedSuffersCriticalHits(ped, false)
        table.insert(combatpeds, ped)
    end

    for k,v in pairs(combatpeds) do
        TaskGoToEntity(v, playerPedId, 5000, 5, 2.5, 1073741824, 0)
    end
    Wait(2000)

    for k,v in pairs(combatpeds) do
        TaskGoToEntityWhileAimingAtEntity(v, playerPedId, playerPedId, 2.5, true, 0, 0, false, false, 'FIRING_PATTERN_FULL_AUTO')
    end
end

function generateCokeLabFunctions(lab)
    if generateLabFunction[lab] then return; end
    local k = lab
    local v = Config.Cocaine.Labs[lab]

    for type, value in pairs(Config.Cocaine.Labs[k].Prep) do
        for key, coord in pairs(value) do
            labZones[key] = TMC.Functions.AddCircleZone('cokelabfunction', coord, 0.8, {
                useZ = true,
                data = {
                    lab = k,
                    labgrade = Config.Cocaine.Labs[k].ProductQualityMultiplier,
                    type = type,
                    station = key,
                    coords = coord,
                }
            })

            TMC.Functions.AddPolyZoneEnterHandler('cokelabfunction', function(data)
                cokelab = data.lab
                cokelabgrade = data.labgrade
                curBarrelCoords = data.coords

                for k,v in pairs(soakingLeaves) do
                    if data.station == v.barrel then
                        curBarrel = v.barrel
                        curIndex = k
                        --print("barrel in soakingleaves", v.barrel)
                        --print("index in soakingleaves", k)
                    end
                end
                if data.type == 'soak' then
                    if data.station == curBarrel then
                        TMC.Functions.ShowPromptGroup(BarrelManagePrompt)
                        TMC.Functions.HidePromptGroup(cokeLabSoakPrompt)
                    else
                        barrelToUse = data.station
                        TMC.Functions.ShowPromptGroup(cokeLabSoakPrompt)
                    end
                elseif data.type == 'prep' then
                    TMC.Functions.ShowPromptGroup(cokeLabPrepPrompt)
                elseif data.type == 'combine' then
                    TMC.Functions.ShowPromptGroup(cokeLabCombinePrompt)
                elseif data.type == 'bag' then
                    TMC.Functions.ShowPromptGroup(cokeLabBagPrompt)
                end
            end)

            TMC.Functions.AddPolyZoneExitHandler('cokelabfunction', function(data)
                curBarrel = false
                curIndex = false
                TMC.Functions.HidePromptGroup(cokeLabSoakPrompt)
                TMC.Functions.HidePromptGroup(cokeLabPrepPrompt)
                TMC.Functions.HidePromptGroup(cokeLabCombinePrompt)
                TMC.Functions.HidePromptGroup(cokeLabBagPrompt)
                TMC.Functions.HidePromptGroup(BarrelManagePrompt)
            end)
        end
    end
    generateLabFunction[lab] = true
end

function cokeLabMenu(title, namespace, elements, type)
    TMC.Functions.OpenMenu({
        namespace = namespace,
        title = title,
        subtitle = "",
        form = true,
        disableFormButtons = true,
    }, elements, function(close)
    end, function(select)
    end, function(change)
        if change.elementChanged == "process" then
            if next(curIngredients) ~= nil then
                TMC.Functions.CloseMenu()
                UpdateMenu({action = "process", stationtype = type})
            else
                TMC.Functions.SimpleNotify("There are no ingredients in here", "error")
            end
        else
            UpdateMenu({action = "update", stationtype = type, amount = change.newValue, item = change.elementChanged})
        end
    end)
end

function LabSoak()
    local elements = {}
    local items = {}

    table.insert(elements, {
        type = "button",
        name = "process",
        label = "Process",
        icon = "fas fa-check",
        description = "Start Soaking Process"
    })
    for id, item in pairs(Config.Cocaine.LabOptions.Useable_Items) do
        if TMC.Functions.HasItem(item) then
            items[TMC.Shared.Items[item].label] = {item = item, amount = TMC.Functions.GetItemAmountByName(item)}
        end
    end
    for id, item in TMC.Common.TableSortAlphabetical(items) do
        table.insert(elements, {
            type = "number",
            name = item.item,
            max = item.amount,
            label = id,
        })
    end
    cokeLabMenu('Cocaine - Ingredients', 'cokesoak', elements, 'soak')
end

function startLabSoak()
    isProcessing = true
    local data = curIngredients
    if next(data) == nil then
        TMC.Functions.SimpleNotify("You didn't throw any ingredients in yet", "error", 1000 * 3)
        return
    end
    local badLeafs = TMC.Functions.GetItemsByPredicate(function(item)
        if item and item.name == 'cokeleaf' and TMC.Common.GetItemQuality(item.info) <= 0 then
            return true
        end
    end)
    Citizen.Wait(500)
    if badLeafs ~= nil and #badLeafs > 0 then
        TMC.Functions.SimpleNotify("You have bad leaves on you, Get rid of them!", "error", 1000 * 3)
        return
    end

    local minLeafs = 5
    local maxLeafs = 30
    curIngredients = {}

    local fail = false
    local reason = 0
    local purity = 0
    local bestRecipe = false
    if data['acid'] == nil then fail = true; reason = 1; end
    if data['salt_bag'] == nil then fail = true; reason = 1; end
    if data['thionylchloride'] ~= nil then fail = true; reason = 1; end
    if data['junk_energy'] ~= nil then fail = true; reason = 1; end
    if data['weed_nutrition'] ~= nil then fail = true; reason = 1; end
    if data['bandage'] ~= nil then fail = true; reason = 1; end
    if data['weed_whitewidow_bag'] ~= nil then fail = true; reason = 1; end
    if data['weed_amnesia_bag'] ~= nil then fail = true; reason = 1; end
    if data['weed_ak47_bag'] ~= nil then fail = true; reason = 1; end
    if data['cokeleaf'] < minLeafs then fail = true; reason = 2; end
    if data['cokeleaf'] > maxLeafs then TMC.Functions.SimpleNotify("This barrel can't hold that many leaves.", "error", 1000 * 3) return; end

    local itemCount = 0
    for k,v in pairs(data) do
        if TMC.IsDev then
            --print("item", k, v)
        end
        itemCount = itemCount + 1
    end

    if not fail then
        local acidtoleaf = data['acid'] / data['cokeleaf']
        if acidtoleaf == 0.2 and data['cokeleaf'] >= 20 then
            purity = 25 * cokelabgrade
            bestRecipe = true
        elseif acidtoleaf == 0.2 and data['cokeleaf'] == 15 then
            purity = 20 * cokelabgrade
        elseif acidtoleaf == 0.2 and data['cokeleaf'] == 10 then
            purity = 15 * cokelabgrade
        elseif acidtoleaf == 0.2 then
            purity = 13 * cokelabgrade
        elseif acidtoleaf >= 0.4 then
            fail, reason = true, 2
        elseif (acidtoleaf > 0.2) or (acidtoleaf < 0.2) then
            purity = 12.5 * cokelabgrade
        end

        if itemCount > 3 and itemCount <= 5 then
            purity = 7 * cokelabgrade
        elseif itemCount > 5 then
            purity = 1.5 * cokelabgrade
        end

        if productionrep >= 1000 then
            purity = purity * 2
        elseif productionrep >= 700 then
            purity = purity * 1.75
        elseif productionrep >= 500 then
            purity = purity * 1.5
        elseif productionrep >= 350 then
            purity = purity * 1.25
        end
    end

    TMC.Functions.LoadAnimDict('amb@prop_human_bum_bin@idle_a')
    TaskPlayAnim(playerPedId, 'amb@prop_human_bum_bin@idle_a', 'idle_a', 8.0, -8.0, 1000 * 20, 1, 0, false, false, false)
    TMC.Functions.ProgressBar(function(success)
        if success then
            --TMC.Functions.TriggerServerEvent("TMC:Server:RemoveItem", 'cokeleaf', minLeafs)
            for k,v in pairs(data) do
                while v > 5 do
                    TMC.Functions.TriggerServerEvent("TMC:Server:RemoveItem", k, 5)
                    v = v - 5
                    Citizen.Wait(200)
                end
                TMC.Functions.TriggerServerEvent("TMC:Server:RemoveItem", k, v)
            end
            if fail then
                stationError(reason)
                isProcessing = false
            else
                local itemData = {
                    purity = purity,
                    creator = LocalPlayer.state.citizenid,
                    bestRecipe = bestRecipe
                }
                TMC.Functions.TriggerServerEvent('drugs:cokeloadBarrel', curBarrelCoords, GetNameOfZone(GetEntityCoords(playerPedId)), itemData.purity, itemData.bestRecipe, barrelToUse)
                TMC.Functions.ShowPromptGroup(BarrelManagePrompt)
                if acidtoleaf == 0.2 and data['cokeleaf'] >= 20 then
                    randomRepChance('cokeproduction', 25)
                else
                    randomRepChance('cokeproduction', 10)
                end
                isProcessing = false
            end

        end
        ClearPedTasksImmediately(playerPedId)
    end, 1000 * 15, "Soaking Leaves")
    TMC.Functions.ShowPromptGroup(cokeLabSoakPrompt)
end

function LabPrep()
    local elements = {}
    local items = {}
    table.insert(elements, {
        type = "subtitle",
        label = "Note - This process uses 1 Cocaine Leaf Paste"
    })
    table.insert(elements, {
        type = "button",
        name = "process",
        label = "Process",
        icon = "fas fa-check",
        description = "Start Preping Process"
    })
    for id, item in pairs(Config.Cocaine.LabOptions.Useable_Items) do
        if TMC.Functions.HasItem(item) then
            items[TMC.Shared.Items[item].label] = {item = item, amount = TMC.Functions.GetItemAmountByName(item)}
        end
    end
    for id, item in TMC.Common.TableSortAlphabetical(items) do
        table.insert(elements, {
            type = "number",
            name = item.item,
            max = item.amount,
            label = id,
        })
    end
    cokeLabMenu('Cocaine - Ingredients', 'cokeprep', elements, 'prep')
end

function startLabPrep()
    isProcessing = true
    local data = curIngredients
    local bucketSlot, bucketSlotIndex, bucketitem = GetFirstItemSlot('cokeleaf_paste')
    local bucketpurity
    local bestRecipe
    if bucketitem.info.purity then
        bucketpurity = bucketitem.info.purity
        if TMC.IsDev then
            print("bucket purity = ", bucketpurity)
        end
    else
        bucketpurity = 13
        --print("bucket purity = 13")
    end

    if bucketitem.info.bestRecipe then
        bestRecipe = bucketitem.info.bestRecipe
    else
        bestRecipe = false
    end

    if next(data) == nil then
        TMC.Functions.SimpleNotify("You didn't throw any ingredients in yet", "error", 1000 * 3)
        return
    end
    local badPaste = TMC.Functions.GetItemsByPredicate(function(item)
        if item and item.name == 'cokeleaf_paste' and TMC.Common.GetItemQuality(item.info) <= 0 then
            return true
        end
    end)
    Citizen.Wait(500)
    if badPaste ~= nil and #badPaste > 0 then
        TMC.Functions.SimpleNotify("You have bad paste on you, Get rid of it!", "error", 1000 * 3)
        return
    end
    curIngredients = {}

    local fail = false
    local reason = 0
    local purity = 0
    if data['cement'] == nil then fail = true; reason = 1; end
    if data['weapon_petrolcan'] == nil then
        fail = true
        reason = 1
    else
        local cansInMix = getCurrentFuelCansInMix(data)
        if cansInMix < 0.99 or cansInMix > 1.01 then
            fail = true
            reason = 2
        end
    end
    if data['thionylchloride'] ~= nil then fail = true; reason = 1; end
    if data['junk_energy'] ~= nil then fail = true; reason = 1; end
    if data['weed_nutrition'] ~= nil then fail = true; reason = 1; end
    if data['bandage'] ~= nil then fail = true; reason = 1; end
    if data['weed_whitewidow_bag'] ~= nil then fail = true; reason = 1; end
    if data['weed_amnesia_bag'] ~= nil then fail = true; reason = 1; end
    if data['weed_ak47_bag'] ~= nil then fail = true; reason = 1; end

    local itemCount = 0
    for k,v in pairs(data) do
        --print("item", k, v)
        itemCount = itemCount + 1
    end
    --print("item count", itemCount)

    if not fail then
        local cementtobucket = data['cement'] / 1
        if cementtobucket == 3.0 then
            purity = 36 * cokelabgrade
        elseif cementtobucket >= 4 then
            fail, reason = true, 2
        elseif (cementtobucket > 3) or (cementtobucket < 3) then
            bestRecipe = false
            purity = 12.5 * cokelabgrade

        end


        if itemCount > 3 and itemCount <= 5 then
            purity = 7 * cokelabgrade
            bestRecipe = false
        elseif itemCount > 5 then
            purity = 1.5 * cokelabgrade
            bestRecipe = false
        end

        --print("cementtobucket", cementtobucket)
        if productionrep >= 1000 then
            purity = purity * 2
        elseif productionrep >= 700 then
            purity = purity * 1.75
        elseif productionrep >= 500 then
            purity = purity * 1.5
        elseif productionrep >= 350 then
            purity = purity * 1.25
        end

        purity = (purity + bucketpurity) / 1.9
        if TMC.IsDev then
            print("purity of extract", purity)
        end

    end

    TMC.Functions.LoadAnimDict('anim@amb@business@coc@coc_unpack_cut@')
    TaskPlayAnim(playerPedId, 'anim@amb@business@coc@coc_unpack_cut@', 'fullcut_cycle_cokepacker', 8.0, -8.0, 1000 * 30, 1, 0.45, false, false, false)
    TMC.Functions.ProgressBar(function(success)
        if success then
            TMC.Functions.TriggerServerEvent("TMC:Server:RemoveItem", 'cokeleaf_paste', 1, bucketSlot, bucketSlotIndex)
            for k,v in pairs(data) do
                TMC.Functions.TriggerServerEvent("TMC:Server:RemoveItem", k, v)
            end
            if fail then
                stationError(reason)
                isProcessing = false
            else
                local itemData = {
                    purity = purity,
                    creator = LocalPlayer.state.citizenid,
                    bestRecipe = bestRecipe
                }
                TMC.Functions.TriggerServerEvent('TMC:Server:AddItem', 'cokechemicalslush', 1, nil, itemData)
                if cementtobucket == 3.0 then
                    randomRepChance('cokeproduction', 33)
                else
                    randomRepChance('cokeproduction', 23)
                end
                isProcessing = false
            end
        end
        ClearPedTasksImmediately(playerPedId)
        TMC.Functions.ShowPromptGroup(cokeLabPrepPrompt)
    end, 1000 * 25, "Preparing Extract")
end

function LabCombine()
    local elements = {}
    local items = {}
    table.insert(elements, {
        type = "subtitle",
        label = "Note - This process uses 1 Cocaine Chemical Extract"
    })
    table.insert(elements, {
        type = "button",
        name = "process",
        label = "Process",
        icon = "fas fa-check",
        description = "Start Chemical Combination Process"
    })
    for id, item in pairs(Config.Cocaine.LabOptions.Useable_Items) do
        if TMC.Functions.HasItem(item) then
            items[TMC.Shared.Items[item].label] = {item = item, amount = TMC.Functions.GetItemAmountByName(item)}
        end
    end
    for id, item in TMC.Common.TableSortAlphabetical(items) do
        table.insert(elements, {
            type = "number",
            name = item.item,
            max = item.amount,
            label = id,
        })
    end
    cokeLabMenu('Cocaine - Ingredients', 'cokecombine', elements, 'combine')
end

function startLabCombine()
    isProcessing = true
    local data = curIngredients
    local bucketSlot, bucketSlotIndex, bucketitem = GetFirstItemSlot('cokechemicalslush')
    local bucketpurity
    local bestRecipe
    if bucketitem.info.purity then
        bucketpurity = bucketitem.info.purity
        if TMC.IsDev then
            print("bucket purity =", bucketpurity)
        end
    else
        bucketpurity = 13
    end

    if bucketitem.info.bestRecipe then
        bestRecipe = bucketitem.info.bestRecipe
    else
        bestRecipe = false
    end

    if next(data) == nil then
        TMC.Functions.SimpleNotify("You didn't throw any ingredients in yet", "error", 1000 * 3)
        return
    end
    local badExtract = TMC.Functions.GetItemsByPredicate(function(item)
        if item and item.name == 'cokechemicalslush' and TMC.Common.GetItemQuality(item.info) <= 0 then
            return true
        end
    end)
    Citizen.Wait(500)
    if badExtract ~= nil and #badExtract > 0 then
        TMC.Functions.SimpleNotify("You have bad extract on you, Get rid of it!", "error", 1000 * 3)
        return
    end
    curIngredients = {}

    local fail = false
    local reason = 0
    local purity = 0
    if data['bottle_ammonia'] == nil and data['ajax_spray'] == nil then fail = true; reason = 1; end
    if not fail then
        local totalammonia = 0
        if data['bottle_ammonia'] ~= nil then totalammonia = totalammonia + (data['bottle_ammonia'] * 3) end
        if data['ajax_spray'] ~= nil then totalammonia = totalammonia + (data['ajax_spray']) end
        if data['thionylchloride'] ~= nil then fail = true; reason = 1; end
        if data['junk_energy'] ~= nil then fail = true; reason = 1; end
        if data['weed_nutrition'] ~= nil then fail = true; reason = 1; end
        if data['bandage'] ~= nil then fail = true; reason = 1; end
        if data['weed_whitewidow_bag'] ~= nil then fail = true; reason = 1; end
        if data['weed_amnesia_bag'] ~= nil then fail = true; reason = 1; end
        if data['weed_ak47_bag'] ~= nil then fail = true; reason = 1; end

        local itemCount = 0
        for k,v in pairs(data) do
            --print("item", k, v)
            itemCount = itemCount + 1
        end

        --print("item count", itemCount)
        --print("total ammonia", totalammonia)
        local ammoniatoslush = totalammonia / 1
        if ammoniatoslush == 12 and not data['ajax_spray'] then
            purity = 46 * cokelabgrade
        elseif ammoniatoslush >= 9 and not data['ajax_spray'] then
            bestRecipe = false
            purity = 20 * cokelabgrade
        elseif ammoniatoslush > 12 then
            fail, reason = true, 2
        elseif ammoniatoslush <= 2 then
            fail, reason = true, 2
        elseif (ammoniatoslush > 2) or (ammoniatoslush < 12) then
            bestRecipe = false
            purity = 12.5 * cokelabgrade
        elseif not data['ajax_spray'] then
            fail, reason = true, 1
        end

        --print("ammoniatoslush", ammoniatoslush)
        if itemCount > 3 and itemCount <= 5 then
            bestRecipe = false
            purity = 7 * cokelabgrade
        elseif itemCount > 5 then
            bestRecipe = false
            purity = 1.5 * cokelabgrade
        end

        if data['ajax_spray'] ~= nil then purity = purity / 1.25 end

        if productionrep >= 1000 then
            purity = purity * 2
        elseif productionrep >= 700 then
            purity = purity * 1.75
        elseif productionrep >= 500 then
            purity = purity * 1.5
        elseif productionrep >= 350 then
            purity = purity * 1.25
        end

        purity = (purity + bucketpurity) / 2
        if purity > 100 then
            purity = 100
        end
        if TMC.IsDev then
            print("purity of raw coke", purity)
        end

    end

    TMC.Functions.LoadAnimDict('anim@amb@business@coc@coc_unpack_cut@')
    TaskPlayAnim(playerPedId, 'anim@amb@business@coc@coc_unpack_cut@', 'fullcut_cycle_v7_cokecutter', 8.0, -8.0, 1000 * 45 , 1, 0.3, false, false, false)
    TMC.Functions.ProgressBar(function(success)
        if success then
            TMC.Functions.TriggerServerEvent("TMC:Server:RemoveItem", 'cokechemicalslush', 1, bucketSlot, bucketSlotIndex)
            for k,v in pairs(data) do
                TMC.Functions.TriggerServerEvent("TMC:Server:RemoveItem", k, v)
            end
            if fail then
                stationError(reason)
                isProcessing = false
            else
                if TMC.IsDev then
                    print("best recipe: ", bestRecipe)
                end

                local itemData = {
                    purity = purity,
                    creator = LocalPlayer.state.citizenid,
                    bestRecipe = bestRecipe
                }
                if bestRecipe and purity > 74 and productionrep >= 1000 then
                    TMC.Functions.TriggerServerEvent('TMC:Server:AddItem', 'rawcoke', 2, nil, itemData)
                else
                    TMC.Functions.TriggerServerEvent('TMC:Server:AddItem', 'rawcoke', 1, nil, itemData)
                end
                if ammoniatoslush == 12 and not data['ajax_spray'] then
                    randomRepChance('cokeproduction', 33)
                else
                    randomRepChance('cokeproduction', 23)
                end
                isProcessing = false
            end
        end
        ClearPedTasksImmediately(playerPedId)
        TMC.Functions.ShowPromptGroup(cokeLabCombinePrompt)
    end, 1000 * 40, "Combining Solidifying Chemicals")
end

function startLabBag(jobType)
    if jobType == "single_bag" then
        if not TMC.Functions.HasItem('rawcoke') or not TMC.Functions.HasItem('empty_weed_bag') then return; end

        local badCoke = TMC.Functions.GetItemsByPredicate(function(item)
            if item and item.name == 'rawcoke' and TMC.Common.GetItemQuality(item.info) <= 0 then
                return true
            end
        end)
        Citizen.Wait(500)
        if badCoke ~= nil and #badCoke > 0 then
            TMC.Functions.SimpleNotify("You have bad coke on you, Get rid of it!", "error", 1000 * 3)
            return
        end

        TMC.Functions.LoadAnimDict('anim@amb@business@coc@coc_unpack_cut_left@')
        TaskPlayAnim(playerPedId, 'anim@amb@business@coc@coc_unpack_cut_left@', 'coke_cut_v4_coccutter', 8.0, -8.0, 1000 * 15, 1, 0, false, false, false)
        TMC.Functions.ProgressBar(function(success)
            if success then
                if TMC.Functions.GetItemAmountByName('empty_weed_bag') < 1 then TMC.Functions.SimpleNotify('You don\'t have enough baggies to split this cocaine', 'inform') return; end
                if TMC.Functions.GetItemAmountByName('rawcoke') < 1 then TMC.Functions.SimpleNotify('You don\'t have enough raw coke to split into baggies', 'inform') return; end

                local cokeid, cokeindex, cokeitem = GetFirstItemSlot('rawcoke')
                local purity = 0
                if cokeitem.info.purity then
                    purity = cokeitem.info.purity
                else
                    purity = 13
                end

                TMC.Functions.TriggerServerEvent("TMC:Server:RemoveItem", 'rawcoke', 1, cokeid, cokeindex)
                TMC.Functions.TriggerServerEvent('TMC:Server:RemoveItem', 'empty_weed_bag', 1)

                local itemData = {
                    quality = purity,
                    creator = LocalPlayer.state.citizenid
                }

                if not purity then
                    TMC.Functions.TriggerServerEvent('TMC:Server:AddItem', 'cokebaggy', 1, nil, itemData)
                elseif purity < 25 then
                    TMC.Functions.TriggerServerEvent('TMC:Server:AddItem', 'cokebaggy', 1, nil, itemData)
                elseif purity < 50 then
                    TMC.Functions.TriggerServerEvent('TMC:Server:AddItem', 'cokebaggy_decent', 1, nil, itemData)
                elseif purity < 75 then
                    TMC.Functions.TriggerServerEvent('TMC:Server:AddItem', 'cokebaggy_good', 1, nil, itemData)
                elseif purity < 90 then
                    TMC.Functions.TriggerServerEvent('TMC:Server:AddItem', 'cokebaggy_better', 1, nil, itemData)
                elseif purity <= 100 then
                    TMC.Functions.TriggerServerEvent('TMC:Server:AddItem', 'cokebaggy_best', 1, nil, itemData)
                end

                randomRepChance('cokeproduction', 3)
            end
            ClearPedTasksImmediately(playerPedId)
            TMC.Functions.ShowPromptGroup(cokeLabBagPrompt)
        end, 1000 * 10, "Packing the Baggie")
    elseif jobType == 'five_bags' then
        if not TMC.Functions.HasItem('rawcoke') or not TMC.Functions.HasItem('empty_weed_bag') then return; end

        local badCoke = TMC.Functions.GetItemsByPredicate(function(item)
            if item and item.name == 'rawcoke' and TMC.Common.GetItemQuality(item.info) <= 0 then
                return true
            end
        end)
        Citizen.Wait(500)
        if badCoke ~= nil and #badCoke > 0 then
            TMC.Functions.SimpleNotify("You have bad coke on you, Get rid of it!", "error", 1000 * 3)
            return
        end

        TMC.Functions.LoadAnimDict('anim@amb@business@coc@coc_unpack_cut_left@')
        TaskPlayAnim(playerPedId, 'anim@amb@business@coc@coc_unpack_cut_left@', 'coke_cut_v4_coccutter', 8.0, -8.0, 1000 * 30, 1, 0, false, false, false)
        TMC.Functions.ProgressBar(function(success)
            if success then
                if TMC.Functions.GetItemAmountByName('empty_weed_bag') < 5 then TMC.Functions.SimpleNotify('You don\'t have enough baggies to split this cocaine', 'inform') return; end
                if TMC.Functions.GetItemAmountByName('rawcoke') < 5 then TMC.Functions.SimpleNotify('You don\'t have enough raw coke to split into baggies', 'inform') return; end

                local cokeid, cokeindex, cokeitem = GetFirstItemSlot('rawcoke')
                local pureCount = 0
                local purity = 0 --cokeitem.info.purity

                local items = TMC.Functions.GetPlayerItems()
                for k,v in pairs(items[cokeid]) do
                    if v.info.purity then
                        purity = purity + v.info.purity
                    else
                        purity = purity + 13
                    end
                    pureCount = pureCount + 1

                    if pureCount == 5 then
                        break
                    end
                end

                purity = purity / 5

                if pureCount < 5 then
                    TMC.Functions.SimpleNotify('You don\'t have enough raw coke of the same purity to split into baggies', 'inform')
                    return
                else
                    TMC.Functions.TriggerServerEvent("TMC:Server:RemoveItem", 'rawcoke', 1, cokeid, cokeindex)
                    for i = 1, 4 do
                        TMC.Functions.TriggerServerEvent("TMC:Server:RemoveItem", 'rawcoke', 1, cokeid, cokeindex)
                    end
                    TMC.Functions.TriggerServerEvent('TMC:Server:RemoveItem', 'empty_weed_bag', 5)

                    local itemData = {
                        quality = purity,
                        creator = LocalPlayer.state.citizenid
                    }
                    if not purity then
                        TMC.Functions.TriggerServerEvent('TMC:Server:AddItem', 'cokebaggy', 1, nil, itemData)
                        local slotid, slotindex, itemdata = GetFirstItemSlot('cokebaggy')
                        for i = 1, 4 do
                            TMC.Functions.TriggerServerEvent('TMC:Server:AddItem', 'cokebaggy', 1, slotid, itemData)
                        end
                    elseif purity < 25 then
                        TMC.Functions.TriggerServerEvent('TMC:Server:AddItem', 'cokebaggy', 1, nil, itemData)
                        local slotid, slotindex, itemdata = GetFirstItemSlot('cokebaggy')
                        for i = 1, 4 do
                            TMC.Functions.TriggerServerEvent('TMC:Server:AddItem', 'cokebaggy', 1, slotid, itemData)
                        end
                    elseif purity < 50 then
                        TMC.Functions.TriggerServerEvent('TMC:Server:AddItem', 'cokebaggy_decent', 1, nil, itemData)
                        local slotid, slotindex, itemdata = GetFirstItemSlot('cokebaggy_decent')
                        for i = 1, 4 do
                            TMC.Functions.TriggerServerEvent('TMC:Server:AddItem', 'cokebaggy_decent', 1, slotid, itemData)
                        end
                    elseif purity < 75 then
                        TMC.Functions.TriggerServerEvent('TMC:Server:AddItem', 'cokebaggy_good', 1, nil, itemData)
                        local slotid, slotindex, itemdata = GetFirstItemSlot('cokebaggy_good')
                        for i = 1, 4 do
                            TMC.Functions.TriggerServerEvent('TMC:Server:AddItem', 'cokebaggy_good', 1, slotid, itemData)
                        end
                    elseif purity < 90 then
                        TMC.Functions.TriggerServerEvent('TMC:Server:AddItem', 'cokebaggy_better', 1, nil, itemData)
                        local slotid, slotindex, itemdata = GetFirstItemSlot('cokebaggy_better')
                        for i = 1, 4 do
                            TMC.Functions.TriggerServerEvent('TMC:Server:AddItem', 'cokebaggy_better', 1, slotid, itemData)
                        end
                    elseif purity <= 100 then
                        TMC.Functions.TriggerServerEvent('TMC:Server:AddItem', 'cokebaggy_best', 1, nil, itemData)
                        local slotid, slotindex, itemdata = GetFirstItemSlot('cokebaggy_best')
                        for i = 1, 4 do
                            TMC.Functions.TriggerServerEvent('TMC:Server:AddItem', 'cokebaggy_best', 1, slotid, itemData)
                        end
                    end
                    randomRepChance('cokeproduction', 5)
                end
            end
            ClearPedTasksImmediately(playerPedId)
            TMC.Functions.ShowPromptGroup(cokeLabBagPrompt)
        end, 1000 * 25, "Packing the Baggies")
    elseif jobType == 'ten_bags' then
        if not TMC.Functions.HasItem('rawcoke') or not TMC.Functions.HasItem('empty_weed_bag') then return; end

        local badCoke = TMC.Functions.GetItemsByPredicate(function(item)
            if item and item.name == 'rawcoke' and TMC.Common.GetItemQuality(item.info) <= 0 then
                return true
            end
        end)
        Citizen.Wait(500)
        if badCoke ~= nil and #badCoke > 0 then
            TMC.Functions.SimpleNotify("You have bad coke on you, Get rid of it!", "error", 1000 * 3)
            return
        end

        TMC.Functions.LoadAnimDict('anim@amb@business@coc@coc_unpack_cut_left@')
        TaskPlayAnim(playerPedId, 'anim@amb@business@coc@coc_unpack_cut_left@', 'coke_cut_v4_coccutter', 8.0, -8.0, 1000 * 50, 1, 0, false, false, false)
        TMC.Functions.ProgressBar(function(success)
            if success then
                if TMC.Functions.GetItemAmountByName('empty_weed_bag') < 10 then TMC.Functions.SimpleNotify('You don\'t have enough baggies to split this cocaine', 'inform') return; end
                if TMC.Functions.GetItemAmountByName('rawcoke') < 10 then TMC.Functions.SimpleNotify('You don\'t have enough raw coke to split into baggies', 'inform') return; end

                local cokeid, cokeindex, cokeitem = GetFirstItemSlot('rawcoke')
                local pureCount = 0
                local purity = 0 --cokeitem.info.purity

                local items = TMC.Functions.GetPlayerItems()
                for k,v in pairs(items[cokeid]) do
                    if v.info.purity then
                        purity = purity + v.info.purity
                    else
                        purity = purity + 13
                    end
                    pureCount = pureCount + 1

                    if pureCount == 10 then
                        break
                    end
                end

                purity = purity / 10

                if pureCount < 10 then
                    TMC.Functions.SimpleNotify('You don\'t have enough raw coke of the same purity to split into baggies', 'inform')
                    return
                else
                    TMC.Functions.TriggerServerEvent("TMC:Server:RemoveItem", 'rawcoke', 1, cokeid, cokeindex)
                    for i = 1, 9 do
                        TMC.Functions.TriggerServerEvent("TMC:Server:RemoveItem", 'rawcoke', 1, cokeid, cokeindex)
                    end
                    TMC.Functions.TriggerServerEvent('TMC:Server:RemoveItem', 'empty_weed_bag', 10)

                    local itemData = {
                        quality = purity,
                        creator = LocalPlayer.state.citizenid
                    }

                    if not purity then
                        TMC.Functions.TriggerServerEvent('TMC:Server:AddItem', 'cokebaggy', 1, nil, itemData)
                        local slotid, slotindex, itemdata = GetFirstItemSlot('cokebaggy')
                        for i = 1, 9 do
                            TMC.Functions.TriggerServerEvent('TMC:Server:AddItem', 'cokebaggy', 1, slotid, itemData)
                        end
                    elseif purity < 25 then
                        TMC.Functions.TriggerServerEvent('TMC:Server:AddItem', 'cokebaggy', 1, nil, itemData)
                        local slotid, slotindex, itemdata = GetFirstItemSlot('cokebaggy')
                        for i = 1, 9 do
                            TMC.Functions.TriggerServerEvent('TMC:Server:AddItem', 'cokebaggy', 1, slotid, itemData)
                        end
                    elseif purity < 50 then
                        TMC.Functions.TriggerServerEvent('TMC:Server:AddItem', 'cokebaggy_decent', 1, nil, itemData)
                        local slotid, slotindex, itemdata = GetFirstItemSlot('cokebaggy_decent')
                        for i = 1, 9 do
                            TMC.Functions.TriggerServerEvent('TMC:Server:AddItem', 'cokebaggy_decent', 1, slotid, itemData)
                        end
                    elseif purity < 75 then
                        TMC.Functions.TriggerServerEvent('TMC:Server:AddItem', 'cokebaggy_good', 1, nil, itemData)
                        local slotid, slotindex, itemdata = GetFirstItemSlot('cokebaggy_good')
                        for i = 1, 9 do
                            TMC.Functions.TriggerServerEvent('TMC:Server:AddItem', 'cokebaggy_good', 1, slotid, itemData)
                        end
                    elseif purity < 90 then
                        TMC.Functions.TriggerServerEvent('TMC:Server:AddItem', 'cokebaggy_better', 1, nil, itemData)
                        local slotid, slotindex, itemdata = GetFirstItemSlot('cokebaggy_better')
                        for i = 1, 9 do
                            TMC.Functions.TriggerServerEvent('TMC:Server:AddItem', 'cokebaggy_better', 1, slotid, itemData)
                        end
                    elseif purity <= 100 then
                        TMC.Functions.TriggerServerEvent('TMC:Server:AddItem', 'cokebaggy_best', 1, nil, itemData)
                        local slotid, slotindex, itemdata = GetFirstItemSlot('cokebaggy_best')
                        for i = 1, 9 do
                            TMC.Functions.TriggerServerEvent('TMC:Server:AddItem', 'cokebaggy_best', 1, slotid, itemData)
                        end
                    end
                    randomRepChance('cokeproduction', 10)
                end
            end
            ClearPedTasksImmediately(playerPedId)
            TMC.Functions.ShowPromptGroup(cokeLabBagPrompt)
        end, 1000 * 45, "Packing the Baggies")
    elseif jobType == 'fifteen_bags' then
        if not TMC.Functions.HasItem('rawcoke') or not TMC.Functions.HasItem('empty_weed_bag') then return; end

        local badCoke = TMC.Functions.GetItemsByPredicate(function(item)
            if item and item.name == 'rawcoke' and TMC.Common.GetItemQuality(item.info) <= 0 then
                return true
            end
        end)
        Citizen.Wait(500)
        if badCoke ~= nil and #badCoke > 0 then
            TMC.Functions.SimpleNotify("You have bad coke on you, Get rid of it!", "error", 1000 * 3)
            return
        end

        TMC.Functions.LoadAnimDict('anim@amb@business@coc@coc_unpack_cut_left@')
        TaskPlayAnim(playerPedId, 'anim@amb@business@coc@coc_unpack_cut_left@', 'coke_cut_v4_coccutter', 8.0, -8.0, 1000 * 80, 1, 0, false, false, false)
        TMC.Functions.ProgressBar(function(success)
            if success then
                if TMC.Functions.GetItemAmountByName('empty_weed_bag') < 15 then TMC.Functions.SimpleNotify('You don\'t have enough baggies to split this cocaine', 'inform') return; end
                if TMC.Functions.GetItemAmountByName('rawcoke') < 15 then TMC.Functions.SimpleNotify('You don\'t have enough raw coke to split into baggies', 'inform') return; end

                local cokeid, cokeindex, cokeitem = GetFirstItemSlot('rawcoke')
                local pureCount = 0
                local purity = 0 --cokeitem.info.purity

                local items = TMC.Functions.GetPlayerItems()
                for k,v in pairs(items[cokeid]) do
                    if v.info.purity then
                        purity = purity + v.info.purity
                    else
                        purity = purity + 13
                    end
                    pureCount = pureCount + 1

                    if pureCount == 15 then
                        break
                    end
                end

                purity = purity / 15

                if pureCount < 15 then
                    TMC.Functions.SimpleNotify('You don\'t have enough raw coke of the same purity to split into baggies', 'inform')
                    return
                else
                    TMC.Functions.TriggerServerEvent("TMC:Server:RemoveItem", 'rawcoke', 1, cokeid, cokeindex)
                    for i = 1, 14 do
                        TMC.Functions.TriggerServerEvent("TMC:Server:RemoveItem", 'rawcoke', 1, cokeid, cokeindex)
                    end
                    TMC.Functions.TriggerServerEvent('TMC:Server:RemoveItem', 'empty_weed_bag', 15)

                    local itemData = {
                        quality = purity,
                        creator = LocalPlayer.state.citizenid
                    }

                    if not purity then
                        TMC.Functions.TriggerServerEvent('TMC:Server:AddItem', 'cokebaggy', 1, nil, itemData)
                        local slotid, slotindex, itemdata = GetFirstItemSlot('cokebaggy')
                        for i = 1, 14 do
                            TMC.Functions.TriggerServerEvent('TMC:Server:AddItem', 'cokebaggy', 1, slotid, itemData)
                        end
                    elseif purity < 25 then
                        TMC.Functions.TriggerServerEvent('TMC:Server:AddItem', 'cokebaggy', 1, nil, itemData)
                        local slotid, slotindex, itemdata = GetFirstItemSlot('cokebaggy')
                        for i = 1, 14 do
                            TMC.Functions.TriggerServerEvent('TMC:Server:AddItem', 'cokebaggy', 1, slotid, itemData)
                        end
                    elseif purity < 50 then
                        TMC.Functions.TriggerServerEvent('TMC:Server:AddItem', 'cokebaggy_decent', 1, nil, itemData)
                        local slotid, slotindex, itemdata = GetFirstItemSlot('cokebaggy_decent')
                        for i = 1, 14 do
                            TMC.Functions.TriggerServerEvent('TMC:Server:AddItem', 'cokebaggy_decent', 1, slotid, itemData)
                        end
                    elseif purity < 75 then
                        TMC.Functions.TriggerServerEvent('TMC:Server:AddItem', 'cokebaggy_good', 1, nil, itemData)
                        local slotid, slotindex, itemdata = GetFirstItemSlot('cokebaggy_good')
                        for i = 1, 14 do
                            TMC.Functions.TriggerServerEvent('TMC:Server:AddItem', 'cokebaggy_good', 1, slotid, itemData)
                        end
                    elseif purity < 90 then
                        TMC.Functions.TriggerServerEvent('TMC:Server:AddItem', 'cokebaggy_better', 1, nil, itemData)
                        local slotid, slotindex, itemdata = GetFirstItemSlot('cokebaggy_better')
                        for i = 1, 14 do
                            TMC.Functions.TriggerServerEvent('TMC:Server:AddItem', 'cokebaggy_better', 1, slotid, itemData)
                        end
                    elseif purity <= 100 then
                        TMC.Functions.TriggerServerEvent('TMC:Server:AddItem', 'cokebaggy_best', 1, nil, itemData)
                        local slotid, slotindex, itemdata = GetFirstItemSlot('cokebaggy_best')
                        for i = 1, 14 do
                            TMC.Functions.TriggerServerEvent('TMC:Server:AddItem', 'cokebaggy_best', 1, slotid, itemData)
                        end
                    end
                    randomRepChance('cokeproduction', 25)
                end
            end
            ClearPedTasksImmediately(playerPedId)
            TMC.Functions.ShowPromptGroup(cokeLabBagPrompt)
        end, 1000 * 75, "Packing the Baggies")
    elseif jobType == '25_bags' then
        if not TMC.Functions.HasItem('rawcoke') or not TMC.Functions.HasItem('empty_weed_bag') then return; end

        local badCoke = TMC.Functions.GetItemsByPredicate(function(item)
            if item and item.name == 'rawcoke' and TMC.Common.GetItemQuality(item.info) <= 0 then
                return true
            end
        end)
        Citizen.Wait(500)
        if badCoke ~= nil and #badCoke > 0 then
            TMC.Functions.SimpleNotify("You have bad coke on you, Get rid of it!", "error", 1000 * 3)
            return
        end

        TMC.Functions.LoadAnimDict('anim@amb@business@coc@coc_unpack_cut_left@')
        TaskPlayAnim(playerPedId, 'anim@amb@business@coc@coc_unpack_cut_left@', 'coke_cut_v4_coccutter', 8.0, -8.0, 1000 * 95, 1, 0, false, false, false)
        TMC.Functions.ProgressBar(function(success)
            if success then
                if TMC.Functions.GetItemAmountByName('empty_weed_bag') < 25 then TMC.Functions.SimpleNotify('You don\'t have enough baggies to split this cocaine', 'inform') return; end
                if TMC.Functions.GetItemAmountByName('rawcoke') < 25 then TMC.Functions.SimpleNotify('You don\'t have enough raw coke to split into baggies', 'inform') return; end

                local cokeid, cokeindex, cokeitem = GetFirstItemSlot('rawcoke')
                local pureCount = 0
                local purity = 0 --cokeitem.info.purity

                local items = TMC.Functions.GetPlayerItems()
                for k,v in pairs(items[cokeid]) do
                    if v.info.purity then
                        purity = purity + v.info.purity
                    else
                        purity = purity + 13
                    end
                    pureCount = pureCount + 1

                    if pureCount == 25 then
                        break
                    end
                end

                purity = purity / 25

                if pureCount < 25 then
                    TMC.Functions.SimpleNotify('You don\'t have enough raw coke of the same purity to split into baggies', 'inform')
                    return
                else
                    TMC.Functions.TriggerServerEvent("TMC:Server:RemoveItem", 'rawcoke', 1, cokeid, cokeindex)
                    for i = 1, 24 do
                        TMC.Functions.TriggerServerEvent("TMC:Server:RemoveItem", 'rawcoke', 1, cokeid, cokeindex)
                    end
                    TMC.Functions.TriggerServerEvent('TMC:Server:RemoveItem', 'empty_weed_bag', 25)

                    local itemData = {
                        quality = purity,
                        creator = LocalPlayer.state.citizenid
                    }

                    if not purity then
                        TMC.Functions.TriggerServerEvent('TMC:Server:AddItem', 'cokebaggy', 1, nil, itemData)
                        local slotid, slotindex, itemdata = GetFirstItemSlot('cokebaggy')
                        for i = 1, 24 do
                            TMC.Functions.TriggerServerEvent('TMC:Server:AddItem', 'cokebaggy', 1, slotid, itemData)
                        end
                    elseif purity < 25 then
                        TMC.Functions.TriggerServerEvent('TMC:Server:AddItem', 'cokebaggy', 1, nil, itemData)
                        local slotid, slotindex, itemdata = GetFirstItemSlot('cokebaggy')
                        for i = 1, 24 do
                            TMC.Functions.TriggerServerEvent('TMC:Server:AddItem', 'cokebaggy', 1, slotid, itemData)
                        end
                    elseif purity < 50 then
                        TMC.Functions.TriggerServerEvent('TMC:Server:AddItem', 'cokebaggy_decent', 1, nil, itemData)
                        local slotid, slotindex, itemdata = GetFirstItemSlot('cokebaggy_decent')
                        for i = 1, 24 do
                            TMC.Functions.TriggerServerEvent('TMC:Server:AddItem', 'cokebaggy_decent', 1, slotid, itemData)
                        end
                    elseif purity < 75 then
                        TMC.Functions.TriggerServerEvent('TMC:Server:AddItem', 'cokebaggy_good', 1, nil, itemData)
                        local slotid, slotindex, itemdata = GetFirstItemSlot('cokebaggy_good')
                        for i = 1, 24 do
                            TMC.Functions.TriggerServerEvent('TMC:Server:AddItem', 'cokebaggy_good', 1, slotid, itemData)
                        end
                    elseif purity < 90 then
                        TMC.Functions.TriggerServerEvent('TMC:Server:AddItem', 'cokebaggy_better', 1, nil, itemData)
                        local slotid, slotindex, itemdata = GetFirstItemSlot('cokebaggy_better')
                        for i = 1, 24 do
                            TMC.Functions.TriggerServerEvent('TMC:Server:AddItem', 'cokebaggy_better', 1, slotid, itemData)
                        end
                    elseif purity <= 100 then
                        TMC.Functions.TriggerServerEvent('TMC:Server:AddItem', 'cokebaggy_best', 1, nil, itemData)
                        local slotid, slotindex, itemdata = GetFirstItemSlot('cokebaggy_best')
                        for i = 1, 24 do
                            TMC.Functions.TriggerServerEvent('TMC:Server:AddItem', 'cokebaggy_best', 1, slotid, itemData)
                        end
                    end
                    randomRepChance('cokeproduction', 75)
                end
            end
            ClearPedTasksImmediately(playerPedId)
            TMC.Functions.ShowPromptGroup(cokeLabBagPrompt)
        end, 1000 * 90, "Packing the Baggies")
    elseif jobType == 'box' then
        if not TMC.Functions.HasItem('rawcoke') or not TMC.Functions.HasItem('packingbox') then return; end
        TMC.Functions.LoadAnimDict('anim@amb@business@coc@coc_unpack_cut_left@')
        TaskPlayAnim(playerPedId, 'anim@amb@business@coc@coc_unpack_cut_left@', 'coke_cut_v4_coccutter', 8.0, -8.0, 1000 * 55, 1, 0, false, false, false)

        local badCoke = TMC.Functions.GetItemsByPredicate(function(item)
            if item and item.name == 'rawcoke' and TMC.Common.GetItemQuality(item.info) <= 0 then
                return true
            end
        end)
        Citizen.Wait(500)
        if badCoke ~= nil and #badCoke > 0 then
            TMC.Functions.SimpleNotify("You have bad coke on you, Get rid of it!", "error", 1000 * 3)
            return
        end

        TMC.Functions.ProgressBar(function(success)
            if success then
            if TMC.Functions.GetItemAmountByName('rawcoke') < 20 then TMC.Functions.SimpleNotify('You don\'t have enough raw coke to brick up', 'inform') return; end

            local cokeid, cokeindex, cokeitem = GetFirstItemSlot('rawcoke')
            local pureCount = 0
            local purity = 0

            local items = TMC.Functions.GetPlayerItems()
            for k,v in pairs(items[cokeid]) do
                if v.info.purity then
                    purity = purity + v.info.purity
                else
                    purity = purity + 13
                end
                pureCount = pureCount + 1

                if pureCount == 20 then
                    break
                end
            end

            purity = purity / 20

            if TMC.IsDev then
                print("brick purity: ", purity)
            end
            if pureCount < 20 then
                TMC.Functions.SimpleNotify('You don\'t have enough raw coke of the same purity to brick', 'inform')
                return
            else
                TMC.Functions.TriggerServerEvent("TMC:Server:RemoveItem", 'rawcoke', 1, cokeid, cokeindex)
                for i = 1, 19 do
                    TMC.Functions.TriggerServerEvent("TMC:Server:RemoveItem", 'rawcoke', 1, cokeid, cokeindex)
                end
                    TMC.Functions.TriggerServerEvent("TMC:Server:RemoveItem", 'packingbox', 1)
                    local itemData = {
                        quality = purity,
                        creator = LocalPlayer.state.citizenid
                    }
                    TMC.Functions.TriggerServerEvent('TMC:Server:AddItem', 'coke_brick', 1, nil, itemData)
                    randomRepChance('cokeproduction', 90)
                end
            end
            ClearPedTasksImmediately(playerPedId)
            TMC.Functions.ShowPromptGroup(cokeLabBagPrompt)
        end, 1000 * 50, "Packing the Box")
    elseif jobType == 'bag' then
        if not TMC.Functions.HasItem('coke_brick') then return; end
        local cokeid, cokeindex, cokeitem = GetFirstItemSlot('coke_brick')
        local cokeQuality = cokeitem.info.quality
        if TMC.IsDev then
            print("quality after bricking:", cokeQuality)
        end
        TMC.Functions.TriggerServerEvent('drugs:cokebag', cokeitem)
        TMC.Functions.ShowPromptGroup(cokeLabBagPrompt)
    end
end

function stationError(type)
    if type == 1 then -- Not Correct Items
        TMC.Functions.SimpleNotify('This batch is ruined! It looks like you didn\'t put the right items in it, or added something you shouldn\'t have', 'error', 1000 * 10)
    elseif type == 2 then -- To Much of an Item
        TMC.Functions.SimpleNotify('This batch is ruined! It looks like you didn\'t put enough/to much of an item in.', 'error', 1000 * 10)
    end
end

function UpdateMenu(data)
    if data.action == "update" then
        curIngredients[data.item] = data.amount
        if curIngredients[data.item] == 0 then
            curIngredients[data.item] = nil
        end
    elseif data.action == "process" then
        if data.stationtype == "soak" then startLabSoak(); TMC.Functions.HidePromptGroup(cokeLabSoakPrompt); end
        if data.stationtype == "prep" then startLabPrep(); TMC.Functions.HidePromptGroup(cokeLabPrepPrompt); end
        if data.stationtype == "combine" then startLabCombine(); TMC.Functions.HidePromptGroup(cokeLabCombinePrompt); end
    end
end

RegisterNetEvent('drugs:cokebrickbag', function(cokequality, slot, index)
    if TMC.Functions.IsInZone('cokelabfunction') then
        if TMC.Functions.GetItemAmountByName('plastic_baggies') < 1 then TMC.Functions.SimpleNotify('You don\'t have enough baggies to split this brick', 'inform') return; end
        TMC.Functions.TriggerServerEvent('TMC:Server:RemoveItem', 'coke_brick', 1, slot, index)
        Citizen.Wait(300)
        TMC.Functions.LoadAnimDict('anim@amb@business@coc@coc_unpack_cut_left@')
        TaskPlayAnim(playerPedId, 'anim@amb@business@coc@coc_unpack_cut_left@', 'coke_cut_v4_coccutter', 8.0, -8.0, 1000 * 62, 1, 0, false, false, false)
        TMC.Functions.ProgressBar(function(success)
            StopAnimTask(playerPedId, "amb@prop_human_bum_bin@idle_a", "idle_a", 1.0)
            if success then
                TMC.Functions.TriggerServerEvent('TMC:Server:RemoveItem', 'plastic_baggies', 1)

                local itemData = {
                    quality = cokequality,
                    creator = LocalPlayer.state.citizenid
                }

                if not cokequality then
                    TMC.Functions.TriggerServerEvent('TMC:Server:AddItem', 'cokebaggy', 1, nil, itemData)
                    local slotid, slotindex, itemdata = GetFirstItemSlot('cokebaggy')
                    for i = 1, 24 do
                        TMC.Functions.TriggerServerEvent('TMC:Server:AddItem', 'cokebaggy', 1, slotid, itemData)
                    end
                elseif cokequality < 25 then
                    TMC.Functions.TriggerServerEvent('TMC:Server:AddItem', 'cokebaggy', 1, nil, itemData)
                    local slotid, slotindex, itemdata = GetFirstItemSlot('cokebaggy')
                    for i = 1, 24 do
                        TMC.Functions.TriggerServerEvent('TMC:Server:AddItem', 'cokebaggy', 1, slotid, itemData)
                    end
                elseif cokequality < 50 then
                    TMC.Functions.TriggerServerEvent('TMC:Server:AddItem', 'cokebaggy_decent', 1, nil, itemData)
                    local slotid, slotindex, itemdata = GetFirstItemSlot('cokebaggy_decent')
                    for i = 1, 24 do
                        TMC.Functions.TriggerServerEvent('TMC:Server:AddItem', 'cokebaggy_decent', 1, slotid, itemData)
                    end
                elseif cokequality < 75 then
                    --print('should hit this print')
                    TMC.Functions.TriggerServerEvent('TMC:Server:AddItem', 'cokebaggy_good', 1, nil, itemData)
                    local slotid, slotindex, itemdata = GetFirstItemSlot('cokebaggy_good')
                    for i = 1, 24 do
                        TMC.Functions.TriggerServerEvent('TMC:Server:AddItem', 'cokebaggy_good', 1, slotid, itemData)
                    end
                elseif cokequality < 90 then
                    TMC.Functions.TriggerServerEvent('TMC:Server:AddItem', 'cokebaggy_better', 1, nil, itemData)
                    local slotid, slotindex, itemdata = GetFirstItemSlot('cokebaggy_better')
                    for i = 1, 24 do
                        TMC.Functions.TriggerServerEvent('TMC:Server:AddItem', 'cokebaggy_better', 1, slotid, itemData)
                    end
                elseif cokequality <= 100 then
                    TMC.Functions.TriggerServerEvent('TMC:Server:AddItem', 'cokebaggy_best', 1, nil, itemData)
                    local slotid, slotindex, itemdata = GetFirstItemSlot('cokebaggy_best')
                    for i = 1, 24 do
                        TMC.Functions.TriggerServerEvent('TMC:Server:AddItem', 'cokebaggy_best', 1, slotid, itemData)
                    end
                end
            else
                TMC.Functions.TriggerServerEvent('TMC:Server:AddItem', 'coke_brick', 1, nil, itemData)
            end
        end, 1000 * 60, "Splitting a Brick", "1,2 .... 24,25", {
            disableCarMovement = true,
            disableCombat = true,
            disableMovement = true,
        })
    else
        TMC.Functions.SimpleNotify('It appears that you can not split this brick here', 'inform')
    end
end)

function BinCleanUp()
    SetTimeout(1000 * 60 * 2, function()
        TMC.Functions.DeleteEntity(DropOffBinObject)
        if dropOffBinZone then
            TMC.Functions.RemoveZoneById(dropOffBinZone.id)
        end
        if dropOffPZ then
            TMC.Functions.RemoveZoneById(dropOffPZ.id)
        end
    end)
end

function phoneNotify(message, length, id)
    if id == nil then
        TMC.Functions.Notify({
            message = message,
            length = length,
            icon = '📱'
        })
    else
        TMC.Functions.Notify({
            message = message,
            length = length,
            id = id,
            icon = '📱'
        })
    end
end

RegisterNetEvent('drugs:cokefieldsendinfo', function(field, delay)
    SetTimeout(delay, function()
        --print('in client send info')
        fieldSelected = field
        fieldAccessGranted = true
        fieldAssigned = true
        --cokeAssignedFieldTimeout(GetGameTimer())
        TMC.Functions.TriggerServerEvent('drugs:cokesendharvestorder', field)
    end)
end)

RegisterNetEvent("drugs:cokefieldUpdateFieldData", function(data)
    CokeFieldData = data
    --print("CokefieldData in update:", CokeFieldData)
end)

RegisterNetEvent("drugs:cokefieldShowPromptGroup", function()
    TMC.Functions.ShowPromptGroup(cokeFieldPromptGroup)
end)

RegisterNetEvent("drugs:cokefieldSendAbandonded", function(field, data)
    --print("data: ", data)
    --print("citizen id: ", LocalPlayer.state.citizenid)
    if data == LocalPlayer.state.citizenid then
        if inField == field then
            --TMC.Functions.SimpleNotify('You have harvested enough, don\'t come back without re-paying your dues', 'error', 1000 * 25)
        else
            TMC.Functions.SimpleNotify('You have abandoned your field, don\'t come back without re-paying your dues', 'error', 1000 * 25)
        end
    end
end)

RegisterNetEvent("drugs:cokefieldPendingGroupHarvest", function(pos)
    --print("group harvest pos", pos)
	curHarvest = pos
    TMC.Functions.Notify({
        message = 'You require an additional person to begin harvesting. Stay within the marked area',
        id = 'CokeHarvest',
        persist = true,
        notifType = 'info'
    })
end)

RegisterNetEvent('drugs:cokeLabInfoUpdate', function(data)
    labInfo = data
end)

RegisterNetEvent('drugs:cokesendDropOff', function()
    print('in send dropoff')
    GiveDropOff()
end)

RegisterNetEvent('drugs:cokedropofffail', function()
    if HasDropOff == true then
        local message = 'Shame, I was looking foward to working with you. Guess we will talk about this later. Wait a while before coming back.'
        phoneNotify(message, 1000 * 15)
    end
    BinCleanUp()
    BlipCleanFunction('DropOff')
    HasDropOff = false
    inQueue = false
    dropOffSuccess = false
    Citizen.Wait(300)
end)

function GetFirstItemSlot(name)
    local items = TMC.Functions.GetPlayerItems()
    for slotId,slot in pairs(items) do
        for slotIndex, item in pairs(slot) do
            if item.name == name then
                return slotId, slotIndex, item
            end
        end
    end
end

function randomRepChance(rep, percent)
    local randomNumber = TMC.Common.TrueRandom(1, 100)
    if randomNumber <= percent then
        TMC.Functions.TriggerServerEvent('drugs:addrep', rep)
        Citizen.Wait(300)
        if productionrep == 350 or productionrep == 500 or productionrep == 700 or productionrep == 1000 then
            TMC.Functions.SimpleNotify("You feel like your quality is improving", "success", 1000 * 10)
        end
    end
end

RegisterNetEvent('drugs:unlocklab', function(lab, status)
    Config.Cocaine.Labs[lab].Unlocked = status
end)

RegisterNetEvent('drugs:givekey', function(enscription, type, lab)
    local itemData = {
        enscription = enscription,
        type = type,
        lab = lab
    }
    if TMC.Functions.HasPermission('god') then
        TMC.Functions.TriggerServerEvent('TMC:Server:AddItem', 'mystery_key', 1, nil, itemData)
    end
end)

function hasItemwithInfo(typeoflab, labid)
    if LocalPlayer.state and TMC.Functions.GetPlayerItems() then
        local items = TMC.Functions.GetPlayerItems()
        for k,v in pairs(items) do
            for index, item in pairs(v) do
                if item.name == 'mystery_key' and item.info.type == typeoflab and item.info.lab == labid then
                    return true
                end
            end
        end
    end
    return false
end

Citizen.CreateThread(function()
    AddStateBagChangeHandler('rep', string.format('player:%s', GetPlayerServerId(playerId)), function(bag, key, value)
        if value ~= nil and value['cokeharvest'] then
            if value['cokeharvest'] > 2550 then
                harvestrep = 2550
            else
                harvestrep = value['cokeharvest']
            end
        elseif value ~= nil and value['cokeproduction'] then
            productionrep = value['cokeproduction']
        end
    end)
end)


-- Coke Consumption
local CokeBaggyEffectThread = false

RegisterNetEvent('drugs:cokeconsumption', function(quality, slot, index)
    TMC.Functions.LoadAnimDict('switch@trevor@trev_smoking_meth')
    if TMC.Functions.HasItem('cokebaggy') then
        if CokeBaggyEffectThread then TMC.Functions.SimpleNotify('Do you really need more coke right now?', 'inform') return end
        TaskPlayAnim(playerPedId, "switch@trevor@trev_smoking_meth", "trev_smoking_meth_loop", 8.0, -8.0, 1000 * 10, 49, 0, false, false, false)
        TMC.Functions.ProgressBar(function(success)
            StopAnimTask(playerPedId, "switch@trevor@trev_smoking_meth", "trev_smoking_meth_loop", 1.0)
            if success then
                TMC.Functions.TriggerServerEvent('TMC:Server:RemoveItem', 'cokebaggy', 1, slot, index)
                CokeBaggyEffect(quality)
            end
        end, 1000 * 5, "Snorting Coke", "Sniff Sniff", {
            disableCarMovement = false,
            disableCombat = true,
            disableMovement = false,
        })
    elseif TMC.Functions.HasItem('cokebaggy_decent') then
        if CokeBaggyEffectThread then TMC.Functions.SimpleNotify('Do you really need more coke right now?', 'inform') return end
        TaskPlayAnim(playerPedId, "switch@trevor@trev_smoking_meth", "trev_smoking_meth_loop", 8.0, -8.0, 1000 * 10, 49, 0, false, false, false)
        TMC.Functions.ProgressBar(function(success)
            StopAnimTask(playerPedId, "switch@trevor@trev_smoking_meth", "trev_smoking_meth_loop", 1.0)
            if success then
                TMC.Functions.TriggerServerEvent('TMC:Server:RemoveItem', 'cokebaggy_decent', 1, slot, index)
                CokeBaggyEffect(quality)
            end
        end, 1000 * 5, "Snorting Coke", "Sniff Sniff", {
            disableCarMovement = false,
            disableCombat = true,
            disableMovement = false,
        })
    elseif TMC.Functions.HasItem('cokebaggy_good') then
        if CokeBaggyEffectThread then TMC.Functions.SimpleNotify('Do you really need more coke right now?', 'inform') return end
        TaskPlayAnim(playerPedId, "switch@trevor@trev_smoking_meth", "trev_smoking_meth_loop", 8.0, -8.0, 1000 * 10, 49, 0, false, false, false)
        TMC.Functions.ProgressBar(function(success)
            StopAnimTask(playerPedId, "switch@trevor@trev_smoking_meth", "trev_smoking_meth_loop", 1.0)
            if success then
                TMC.Functions.TriggerServerEvent('TMC:Server:RemoveItem', 'cokebaggy_good', 1, slot, index)
                CokeBaggyEffect(quality)
            end
        end, 1000 * 5, "Snorting Coke", "Sniff Sniff", {
            disableCarMovement = false,
            disableCombat = true,
            disableMovement = false,
        })
    elseif TMC.Functions.HasItem('cokebaggy_better') then
        if CokeBaggyEffectThread then TMC.Functions.SimpleNotify('Do you really need more coke right now?', 'inform') return end
        TaskPlayAnim(playerPedId, "switch@trevor@trev_smoking_meth", "trev_smoking_meth_loop", 8.0, -8.0, 1000 * 10, 49, 0, false, false, false)
        TMC.Functions.ProgressBar(function(success)
            StopAnimTask(playerPedId, "switch@trevor@trev_smoking_meth", "trev_smoking_meth_loop", 1.0)
            if success then
                TMC.Functions.TriggerServerEvent('TMC:Server:RemoveItem', 'cokebaggy_better', 1, slot, index)
                CokeBaggyEffect(quality)
            end
        end, 1000 * 5, "Snorting Coke", "Sniff Sniff", {
            disableCarMovement = false,
            disableCombat = true,
            disableMovement = false,
        })
    elseif TMC.Functions.HasItem('cokebaggy_best') then
        if CokeBaggyEffectThread then TMC.Functions.SimpleNotify('Do you really need more coke right now?', 'inform') return end
        TaskPlayAnim(playerPedId, "switch@trevor@trev_smoking_meth", "trev_smoking_meth_loop", 8.0, -8.0, 1000 * 10, 49, 0, false, false, false)
        TMC.Functions.ProgressBar(function(success)
            StopAnimTask(playerPedId, "switch@trevor@trev_smoking_meth", "trev_smoking_meth_loop", 1.0)
            if success then
                TMC.Functions.TriggerServerEvent('TMC:Server:RemoveItem', 'cokebaggy_best', 1, slot, index)
                CokeBaggyEffect(quality)
            end
        end, 1000 * 5, "Snorting Coke", "Sniff Sniff", {
            disableCarMovement = false,
            disableCombat = true,
            disableMovement = false,
        })
    end
end)

function CokeBaggyEffect(quality)
    if not CokeBaggyEffectThread then
        Citizen.CreateThread(function()
            CokeBaggyEffectThread = true
            local startStamina = 20
            local cokefall = false

            AlienEffect()
            if not quality then
                SetRunSprintMultiplierForPlayer(playerId, 1.01)
            elseif quality <= 20 then
                SetRunSprintMultiplierForPlayer(playerId, 1.01)
            elseif quality <= 40 then
                SetRunSprintMultiplierForPlayer(playerId, 1.10)
            elseif quality <= 50 then
                SetRunSprintMultiplierForPlayer(playerId, 1.125)
            elseif quality <= 70 then
                SetRunSprintMultiplierForPlayer(playerId, 1.15)
            elseif quality <= 100 then
                SetRunSprintMultiplierForPlayer(playerId, 1.25)
            end
            while startStamina > 0 do
                Citizen.Wait(1000)
                if TMC.Common.TrueRandom(1, 100) < 20 then
                    RestorePlayerStamina(playerId, 1.0)
                end
                if TMC.Common.TrueRandom(1, 100) < 10 and IsPedOnFoot(playerPedId) and (IsPedRunning(playerPedId) or IsPedSprinting(playerPedId)) and not IsPedRagdoll(playerPedId) and not cokefall then
                    SetPedToRagdoll(playerPedId, TMC.Common.TrueRandom(1000, 3000), TMC.Common.TrueRandom(1000, 3000), 3)
                    cokefall = true
                end
                startStamina = startStamina - 1
            end
            startStamina = 0
            cokefall = false
            SetRunSprintMultiplierForPlayer(playerId, 1.0)
            CokeBaggyEffectThread = false
        end)
    end
end

function AlienEffect()
    StartScreenEffect("DrugsMichaelAliensFightIn", 3.0, 0)
    Citizen.Wait(TMC.Common.TrueRandom(5000, 8000))
    StartScreenEffect("DrugsMichaelAliensFight", 3.0, 0)
    Citizen.Wait(TMC.Common.TrueRandom(5000, 8000))
    StartScreenEffect("DrugsMichaelAliensFightOut", 3.0, 0)
    StopScreenEffect("DrugsMichaelAliensFightIn")
    StopScreenEffect("DrugsMichaelAliensFight")
    StopScreenEffect("DrugsMichaelAliensFightOut")
end


--Leaf Soak Updates
RegisterNetEvent("drugs:cokeUpdateSoakStage", function(zone, id, stage, coords)
    if GetNameOfZone(GetEntityCoords(playerPedId)) ~= zone then return; end
    if not soakingLeaves then return; end
    if not soakingLeaves[id] then return; end
    --TMC.Functions.DeleteEntity(soakingLeaves[id].object)
    --soakingLeaves[id].object = CreatePlant(stage, coords)
end)

RegisterNetEvent("drugs:cokeaddBarrel", function(zone, id, leafData)
    if GetNameOfZone(GetEntityCoords(playerPedId)) ~= zone then return; end
    soakingLeaves[id] = {}
    soakingLeaves[id].barrel = leafData.barrel
    soakingLeaves[id].index = leafData.index
    soakingLeaves[id].purity = leafData.quality
end)

RegisterNetEvent("drugs:cokeFreeBarrel", function(zone, id)
    curBarrel = nil
    curIndex = nil
    soakingLeaves[id] = nil
end)

RegisterNetEvent("drugs:cokereturnZoneBarrels", function(zone, zoneData)
    if GetNameOfZone(GetEntityCoords(playerPedId)) ~= 'SanAnd' then return; end
    for k,v in pairs(zoneData) do
        soakingLeaves[k] = {}
        soakingLeaves[k].barrel = v.barrel
        soakingLeaves[k].index = v.index
        soakingLeaves[k].purity = v.quality
    end
end)

RegisterNetEvent("drugs:cokereturnLeafData", function(leafData)
    local elements = {}
    table.insert(elements, {
        type = "button",
        name = "status",
        label = "Status:",
        icon = "fas fa-list",
        description = string.format("Progress: %s | Purity: %s", leafData.progress, leafData.quality),
        disabled = true
    })
    if leafData.progress == 100 then
        table.insert(elements, {
            type = "button",
            name = "harvest",
            label = "Harvest",
            icon = "fas fa-hand-holding-seedling",
        })
    end
    TMC.Functions.OpenMenu({
        namespace = "soaking_leaves",
        title = 'soaking leaves',
        subtitle = "Soak info",
    }, elements, function(close)
        ClearPedTasks(playerPedId)
    end, function(select)
    end, function(change)
        TMC.Functions.CloseMenu("soaking_leaves")
        Wait(500)
        if change.elementChanged == "harvest" then
            TMC.Functions.LoadAnimDict('amb@prop_human_bum_bin@idle_a')
            TaskPlayAnim(playerPedId, 'amb@prop_human_bum_bin@idle_a', 'idle_a', 8.0, -8.0, 1000 * 30, 1, 0, false, false, false)
            TMC.Functions.ProgressBar(function(success)
                if success then
                    ClearPedTasks(playerPedId)
                    if fail then
                        stationError(reason)
                        isProcessing = false
                    else
                        local itemData = {
                            purity = leafData.quality,
                            creator = LocalPlayer.state.citizenid,
                            bestRecipe = leafData.bestRecipe
                        }
                        --TMC.Functions.TriggerServerEvent('TMC:Server:AddItem', 'cokeleaf_paste', 1, nil, itemData)
                        TMC.Functions.TriggerServerEvent("drugs:cokeharvestLeafPaste", GetNameOfZone(GetEntityCoords(playerPedId)), curIndex, itemData)
                        isProcessing = false
                    end
                end
                ClearPedTasksImmediately(playerPedId)
            end, 1000 * 15, "Retreiving Leaf Paste")
        end
    end)
end)

RegisterNetEvent("drugs:cokeaddCokeLeafPaste", function(itemData)
    TMC.Functions.TriggerServerEvent('TMC:Server:AddItem', 'cokeleaf_paste', 1, nil, itemData)
end)


Citizen.CreateThread(function()
    BarrelManagePrompt = TMC.Functions.CreatePromptGroup({
        {
            Id = "barrel_manage",
            Complete = function()
                TMC.Functions.TriggerServerEvent("drugs:cokegetLeafData", GetNameOfZone(GetEntityCoords(playerPedId)), curIndex)
                --TaskStartScenarioInPlace(playerPedId, "WORLD_HUMAN_GARDENER_PLANT", 0, true)
            end,
            Title = "Inspect Soaking Leafs",
            Icon = "fas fa-magnifying-glass"
        },
    })
end)

function GetZonePedIsInTick()
    Citizen.CreateThread(function()
        local lastZone = GetNameOfZone(GetEntityCoords(playerPedId))
        TMC.Functions.TriggerServerEvent("drugs:cokegetZoneBarrels", lastZone)
        while isLoggedIn do
            Citizen.Wait(1000)
            local curZone = GetNameOfZone(GetEntityCoords(playerPedId))
            TMC.Functions.TriggerServerEvent("drugs:cokegetZoneBarrels", curZone)
            lastZone = curZone
        end
    end)
end

RegisterNetEvent('TMC:Client:OnPlayerLoaded', function()
    GetZonePedIsInTick()
end)

AddEventHandler("onResourceStart", function(name)
    if GetCurrentResourceName() ~= name then return; end
    if TMC and TMC.Functions.IsPlayerLoaded() then
        GetZonePedIsInTick()
    end
end)

function getCurrentFuelCansInMix(data)
    local petrolCanMaxAmmo = exports['fuel']:getPetrolCanMaxAmmo()
    local ingredients = data

    local totalAmmo = 0

    local fuelInInventory = TMC.Functions.GetItemsByPredicate(function(k)
        if k.name == 'weapon_petrolcan' then
            if  k.info and k.info.ammo then
                return true
            end
        end
    end)

    for name, amount in pairs(ingredients) do
        if name == 'weapon_petrolcan' then

            if amount > #fuelInInventory then
                print('ERROR more fuel selected then present in inventory')
                return 0.0
            end

            for i = 1, amount do
                totalAmmo = totalAmmo + fuelInInventory[i].info.ammo
            end
        end
    end

    -- print("total ammo:", totalAmmo)
    -- print("petrolCanMaxAmmo: ", petrolCanMaxAmmo)
    -- print("final ammo: ", totalAmmo/petrolCanMaxAmmo)
    return totalAmmo / petrolCanMaxAmmo

end
