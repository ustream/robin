/*
 * Copyright (C) 2013 Ustream Inc.
 * author chaotx <lombai.ferenc@ustream.tv>
 *
 * For the full copyright and license information, please view the LICENSE
 * file that was distributed with this source code.
 */
package com.robin.device;

import com.android.ddmlib.AndroidDebugBridge.IDeviceChangeListener;
import com.android.ddmlib.IDevice;

public class RobinDeviceChangeListener implements IDeviceChangeListener
{

    @Override
    public void deviceConnected(final IDevice device)
    {
        if (device.isOnline() && device.getPropertyCount() > 0)
        {
            DevicePool.addDeviceToList(device);
        }
    }

    @Override
    public void deviceDisconnected(final IDevice device)
    {
        DevicePool.removeDeviceFromList(device);
    }

    @Override
    public void deviceChanged(final IDevice device, final int changeMask)
    {
        if (device.isOnline() && device.getPropertyCount() > 0)
        {
            DevicePool.addDeviceToList(device);
        } else
        {
            DevicePool.removeDeviceFromList(device);
        }
    }

}
